
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.logicng.formulas.And;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Not;
import org.logicng.formulas.Or;
import org.logicng.formulas.Variable;
import org.logicng.transformations.simplification.BackboneSimplifier;
import org.logicng.transformations.simplification.NegationSimplifier;

public class QueryTreeOptimizer<BoolFilter extends Filter, Filter> {

    private final Class<BoolFilter> clz;
    private final BooleanClauseReader<BoolFilter, Filter> clauseReader;
    private final EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier;
    private final QueryBuilder<BoolFilter, Filter> queryBuilder;
    private final LeafFilterReducer<Filter> leafFilterReducer;

    private QueryTreeOptimizer(Class<BoolFilter> clz, BooleanClauseReader<BoolFilter, Filter> clauseReader,
                               EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier,
                               QueryBuilder<BoolFilter, Filter> queryBuilder, LeafFilterReducer<Filter> leafFilterReducer) {
        this.clz = clz;
        this.clauseReader = clauseReader;
        this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        this.queryBuilder = queryBuilder;
        this.leafFilterReducer = leafFilterReducer;
    }


    public static FilterBuilder optimizeFilterBuilder(BoolFilterBuilder boolFilterBuilder) {
        EsBoolFilterHelper esBoolFilterHelper = new EsBoolFilterHelper();
        if (!esBoolFilterHelper.initialized()) {
            return boolFilterBuilder;
        }
        return new QueryTreeOptimizer<>(BoolFilterBuilder.class, esBoolFilterHelper, esBoolFilterHelper, esBoolFilterHelper, esBoolFilterHelper)
            .optimize(boolFilterBuilder);
    }

    private Filter optimize(BoolFilter boolFilter) {
        State<Filter> filterState = convertToFormula(boolFilter);
        return createFilter(filterState.formula, filterState);
    }

    private Formula reduceFormula(Formula formula) {
        if (!canReduce(formula)) {
            return formula;
        }
        Formula reducedFormula = new BackboneSimplifier().apply(formula, false);
        if (reducedFormula.numberOfAtoms() >= formula.numberOfAtoms()) {
            return formula;
        } else if (numberOfNegativeNodes(reducedFormula) > numberOfNegativeNodes(formula)) {
            return new NegationSimplifier().apply(reducedFormula, false);
        }
        return reducedFormula;
    }

    private boolean canReduce(Formula formula) {

        // return true only if there are atleast %20 common clauses.
        long total = formula.numberOfAtoms();
        long unique = formula.literals().size();
        return unique * 100 / total <= 80;
    }

    private long numberOfNegativeNodes(Formula formula) {
        long count = 0;
        switch (formula.type()) {
            case NOT:
                count = 1;
                break;
            case LITERAL:
                count = ((Literal) formula).phase() ? 0 : 1;
                break;
        }
        for (Formula value : formula) {
            count += numberOfNegativeNodes(value);
        }
        return count;
    }

    private Filter createFilter(Formula formula, State<Filter> filterState) {
        switch (formula.type()) {
            case AND: {
                And and = (And) formula;
                return reduce(handleCompoundCondition(and.iterator(), BooleanClauseType.MUST, filterState));
            }
            case OR: {
                Or or = (Or) formula;
                return reduce(handleCompoundCondition(or.iterator(), BooleanClauseType.SHOULD, filterState));
            }
            case NOT: {
                Not not = (Not) formula;
                return reduce(handleCompoundCondition(not.iterator(), BooleanClauseType.MUST_NOT, filterState));
            }
            case LITERAL: {
                Literal literal = (Literal) formula;
                boolean phase = literal.phase();
                Filter filter = filterState.getFilter(literal.name());
                if (phase) {
                    return filter;
                } else {
                    BoolFilter boolFilter = queryBuilder.newBoolQuery();
                    queryBuilder.addClause(boolFilter, BooleanClauseType.MUST_NOT, filter);
                    return boolFilter;
                }
            }
            case TRUE:
                return queryBuilder.newMatchAllQuery();
            case FALSE:
                return queryBuilder.newMatchNoneQuery();
        }
        throw new UnsupportedOperationException(formula.type() + " is not supported");
    }

    private Filter reduce(BoolFilter boolFilter) {
        Map<BooleanClauseType, List<Filter>> allClauses = clauseReader.getAllClauses(boolFilter);
        Map<BooleanClauseType, List<Filter>> reducedClauses = new HashMap<>();
        for (Entry<BooleanClauseType, List<Filter>> entry : allClauses.entrySet()) {
            BooleanClauseType type = entry.getKey();
            List<Filter> clauses = entry.getValue();
            Set<Holder<Filter>> leafClauses = new HashSet<>();
            for (Filter clause : clauses) {
                if (clauseReader.isLeafClause(clause)) {
                    leafClauses.add(new Holder<>(clause, equalsAndHashCodeSupplier));
                } else {
                    SprinklrCollectionUtils.addToMultivaluedMapList(reducedClauses, type, clause);
                }
            }
            if (SprinklrCollectionUtils.isNotEmpty(leafClauses)) {
                List<Filter> reducedLeafClauses = reduceLeafClauses(leafClauses.stream().map(h -> h.obj).collect(Collectors.toList()), type);
                if (SprinklrCollectionUtils.isNotEmpty(reducedLeafClauses)) {
                    for (Filter reducedLeafClause : reducedLeafClauses) {
                        SprinklrCollectionUtils.addToMultivaluedMapList(reducedClauses, type, reducedLeafClause);
                    }
                }
            }
        }
        if (reducedClauses.size() == 1) {
            Entry<BooleanClauseType, List<Filter>> entry = reducedClauses.entrySet().iterator().next();
            switch (entry.getKey()) {
                case MUST:
                case SHOULD:
                    if (entry.getValue().size() == 1) {
                        return entry.getValue().get(0);
                    }
            }
        }
        BoolFilter result = queryBuilder.newBoolQuery();
        for (Entry<BooleanClauseType, List<Filter>> entry : reducedClauses.entrySet()) {
            for (Filter filter : entry.getValue()) {
                queryBuilder.addClause(result, entry.getKey(), filter);
            }
        }
        return result;
    }

    private List<Filter> reduceLeafClauses(List<Filter> leafClauses, BooleanClauseType clauseType) {
        if (clauseType == BooleanClauseType.MUST) {
            // not reducing here as this can break in case of multi valued fields.
            return leafClauses;
        }
        return leafFilterReducer.reduce(leafClauses, LeafFilterReducer.Type.UNION);
    }
    private BoolFilter handleCompoundCondition(Iterator<Formula> formulae, BooleanClauseType clauseType, State<Filter> state) {
        BoolFilter result = queryBuilder.newBoolQuery();
        List<BoolFilter> pureNegatives = new ArrayList<>();
        while (formulae.hasNext()) {
            Formula formula = formulae.next();
            Filter filter = createFilter(formula, state);
            assert filter != null;
            boolean addToBool = true;
            if (clz.isAssignableFrom(filter.getClass())) {
                BoolFilter boolFilter = (BoolFilter) filter;
                if (hasOnlyClausesOfSameType(clauseType, boolFilter)) {
                    addToBool = false;
                    List<Filter> clauses = clauseReader.getClauses(boolFilter, clauseType);
                    clauses.forEach(clause -> queryBuilder.addClause(result, clauseType, clause));
                } else if (isPureNegative(boolFilter)) {
                    addToBool = false;
                    pureNegatives.add(boolFilter);
                }
            }
            if (addToBool) {
                queryBuilder.addClause(result, clauseType, filter);
            }
        }
        if (SprinklrCollectionUtils.isNotEmpty(pureNegatives)) {
            if (pureNegatives.size() == 1 || clauseType == BooleanClauseType.MUST) {
                for (BoolFilter boolFilter : pureNegatives) {
                    List<Filter> clauses = clauseReader.getClauses(boolFilter, BooleanClauseType.MUST_NOT);
                    clauses.forEach(clause -> queryBuilder.addClause(result, BooleanClauseType.MUST_NOT, clause));
                }
            } else {
                pureNegatives.forEach(boolFilter -> queryBuilder.addClause(result, clauseType, boolFilter));
            }
        }
        return result;
    }
    private boolean isPureNegative(BoolFilter boolFilter) {
        Map<BooleanClauseType, List<Filter>> allClauses = clauseReader.getAllClauses(boolFilter);
        boolean mustFilterExists = allClauses.containsKey(BooleanClauseType.MUST);
        boolean shouldFilterExists = allClauses.containsKey(BooleanClauseType.SHOULD);
        boolean mustNotFilterExists = allClauses.containsKey(BooleanClauseType.MUST_NOT);
        return !mustFilterExists && !shouldFilterExists && mustNotFilterExists;
    }
    private boolean hasOnlyClausesOfSameType(BooleanClauseType clauseType, BoolFilter boolFilter) {
        Map<BooleanClauseType, List<Filter>> allClauses = clauseReader.getAllClauses(boolFilter);
        boolean mustClausesExists = allClauses.containsKey(BooleanClauseType.MUST);
        boolean shouldClausesExists = allClauses.containsKey(BooleanClauseType.SHOULD);
        boolean mustNotClausesExists = allClauses.containsKey(BooleanClauseType.MUST_NOT);
        switch (clauseType) {
            case MUST:
                return mustClausesExists && !shouldClausesExists && !mustNotClausesExists;
            case MUST_NOT:
                return !mustClausesExists && !shouldClausesExists && mustNotClausesExists;
            case SHOULD:
                return !mustClausesExists && shouldClausesExists && !mustNotClausesExists;
        }
        return false;
    }

    private State<Filter> convertToFormula(BoolFilter boolFilter) {
        State<Filter> state = new State<>(equalsAndHashCodeSupplier);
        state.formula = convertToFormula(boolFilter, state);
        return state;
    }

    private Formula convertToFormula(BoolFilter boolFilter, State<Filter> state) {
        Map<BooleanClauseType, List<Filter>> allClauses = clauseReader.getAllClauses(boolFilter);
        List<Formula> formulas = new ArrayList<>();
        List<Filter> clauses = allClauses.get(BooleanClauseType.MUST);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            List<Formula> andFormulae = addClauses(clauses, state);
            Formula formula = null;
            if (andFormulae.size() > 1) {
                formula = reduceFormula(state.formulaFactory.and(andFormulae));
            } else {
                formula = andFormulae.get(0);
            }
            formulas.add(formula);
        }
        clauses = allClauses.get(BooleanClauseType.SHOULD);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            List<Formula> orFormulae = addClauses(clauses, state);
            Formula formula = null;
            if (orFormulae.size() > 1) {
                formula = reduceFormula(state.formulaFactory.or(orFormulae));
            } else {
                formula = orFormulae.get(0);
            }
            formulas.add(formula);
        }
        clauses = allClauses.get(BooleanClauseType.MUST_NOT);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            List<Formula> orFormulae = addClauses(clauses, state);
            Formula formula = null;
            if (orFormulae.size() > 1) {
                formula = reduceFormula(state.formulaFactory.or(orFormulae));
            } else {
                formula = orFormulae.get(0);
            }
            formulas.add(state.formulaFactory.not(formula));
        }
        return state.formulaFactory.and(formulas);
    }

    private List<Formula> addClauses(List<Filter> clauses, State<Filter> state) {
        List<Formula> formulas = new ArrayList<>();
        for (Filter clause : clauses) {
            if (clz.isAssignableFrom(clause.getClass())) {
                //noinspection unchecked
                formulas.add(convertToFormula((BoolFilter) clause, state));
            } else {
                formulas.add(state.registerAndGetVariable(clause));
            }
        }
        return formulas;
    }

    private static class State<Filter> {

        private FormulaFactory formulaFactory;
        private Formula formula;
        private final Map<Holder<Filter>, String> variables = new HashMap<>();
        private final Map<String, Filter> filterMap = new HashMap<>();
        private final EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier;
        private int varNo = 0;

        public State(EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier) {
            this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
            this.formulaFactory = new FormulaFactory();
        }

        public Variable registerAndGetVariable(Filter filter) {
            Holder<Filter> holder = new Holder<>(filter, equalsAndHashCodeSupplier);
            String var = variables.get(holder);
            if (StringUtils.isBlank(var)) {
                var = "var" + ++varNo;
                variables.put(holder, var);
                filterMap.put(var, filter);
            }
            return formulaFactory.variable(var);
        }

        public Filter getFilter(String var) {
            return filterMap.get(var);
        }
    }

    private static class Holder<T> {

        private final T obj;
        private final EqualsAndHashCodeSupplier<T> equalsAndHashCodeSupplier;

        public Holder(T obj, EqualsAndHashCodeSupplier<T> equalsAndHashCodeSupplier) {
            this.obj = obj;
            this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Holder<T> holder = (Holder<T>) o;
            return equalsAndHashCodeSupplier.areEqual(obj, holder.obj);
        }

        @Override
        public int hashCode() {
            return equalsAndHashCodeSupplier.hashCode(obj);
        }
    }

}
