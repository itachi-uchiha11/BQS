import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.logicng.formulas.*;

import java.util.Map.Entry;

import org.logicng.transformations.simplification.*;

import java.util.*;
import java.util.stream.Collectors;

public class QueryTreeOptimizer<BoolFilter extends Filter, Filter> {

    private final Class<BoolFilter> clz;
    private final BooleanClauseReader<BoolFilter, Filter> clauseReader;
    private final QueryBuilderHelper<BoolFilter, Filter> queryBuilder;
    private final EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier;
    private final LeafQueryHelper<Filter> leafQueryHelper;
    private final BackboneSimplifier backboneSimplifier = new BackboneSimplifier();
    private final FactorOutSimplifier factorOutSimplifier = new FactorOutSimplifier();
    private final NegationSimplifier negationSimplifier = new NegationSimplifier();
    private boolean prefixOn = false;

    private QueryTreeOptimizer(Class<BoolFilter> clz, BooleanClauseReader<BoolFilter, Filter> clauseReader,
                               EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier, QueryBuilderHelper<BoolFilter, Filter> queryBuilder, LeafQueryHelper<Filter> leafQueryHelper, boolean prefixOn) {

        this.clz = clz;
        this.clauseReader = clauseReader;
        this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        this.queryBuilder = queryBuilder;
        this.leafQueryHelper = leafQueryHelper;
        this.prefixOn = prefixOn;

    }

    private QueryTreeOptimizer(Class<BoolFilter> clz, BooleanClauseReader<BoolFilter, Filter> clauseReader,
                               EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier, QueryBuilderHelper<BoolFilter, Filter> queryBuilder, LeafQueryHelper<Filter> leafQueryHelper) {

        this.clz = clz;
        this.clauseReader = clauseReader;
        this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        this.queryBuilder = queryBuilder;
        this.leafQueryHelper = leafQueryHelper;

    }

    // 2 different static factory methods
    // based on whether user wants to enable Prefix Query Optimization
    public static QueryBuilder optimizeBoolQueryBuilder(BoolQueryBuilder boolquerybuilder, boolean prefixOn) {
        EsBoolFilterHelper esBoolFilterHelper = new EsBoolFilterHelper();
        return new QueryTreeOptimizer<>(BoolQueryBuilder.class, esBoolFilterHelper, esBoolFilterHelper, esBoolFilterHelper, esBoolFilterHelper, prefixOn).optimize(boolquerybuilder);
    }

    public static QueryBuilder optimizeBoolQueryBuilder(BoolQueryBuilder boolquerybuilder) {
        EsBoolFilterHelper esBoolFilterHelper = new EsBoolFilterHelper();
        return new QueryTreeOptimizer<>(BoolQueryBuilder.class, esBoolFilterHelper, esBoolFilterHelper, esBoolFilterHelper, esBoolFilterHelper).optimize(boolquerybuilder);
    }

    //Instantiates a new State object
    //calls convertToFormula followed by convertToQuery
    private Filter optimize(BoolFilter boolQueryBuilder) {
        State<Filter> state = new State<>(equalsAndHashCodeSupplier);
        Formula optimized = convertToFormula(boolQueryBuilder, state);
        Filter filter = convertToQuery(optimized, state);
        return filter;
    }

    //Converts the BoolQueryBuilder object into
    //optimized Formula object
    private Formula convertToFormula(BoolFilter boolQueryBuilder, State state) {
        Map<BooleanClauseType, List<Filter>> allclauses = clauseReader.getAllClauses(boolQueryBuilder);

        List<Formula> formulas = new ArrayList<>();

        List<Filter> clauses = allclauses.get(BooleanClauseType.MUST);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            List<Formula> mustFormulae = addClauses(clauses, state);
            Formula formula;
            formula = simplifyFormula(state.formulaFactory.and(mustFormulae));
            formulas.add(formula);
        }

        clauses = allclauses.get(BooleanClauseType.SHOULD);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            List<Formula> shouldFormulae = addClauses(clauses, state);
            Formula formula;
            formula = simplifyFormula(state.formulaFactory.or(shouldFormulae));
            formulas.add(formula);
        }

        clauses = allclauses.get(BooleanClauseType.MUST_NOT);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            List<Formula> mustNotFormulae = addClauses(clauses, state);
            Formula formula;
            formula = simplifyFormula(state.formulaFactory.not(state.formulaFactory.or(mustNotFormulae)));
            formulas.add(formula);
        }

        return state.formulaFactory.and(formulas);
    }

    //returns list of Formula objects
    //from List of QueryBuilder objects
    private List<Formula> addClauses(List<Filter> clauses, State state) {
        List<Formula> formulas = new ArrayList<>();
        for (Filter clause : clauses) {
            if (clz.isAssignableFrom(clause.getClass())) {
                //checks for recursive bool query
                formulas.add(convertToFormula((BoolFilter) clause, state));
            } else {
                //in case of non bool query, register clause and return Formula object
                formulas.add(state.getVariable(clause));
            }
        }

        return formulas;
    }

    //simplifies Formula object into a reduced equivalent Formula object
    private Formula simplifyFormula(Formula formula) {

        formula = backboneSimplifier.apply(formula, false);
        formula = factorOutSimplifier.apply(formula, false);
        formula = negationSimplifier.apply(formula, false);

        return formula;

    }

    //handles compound Formula objects and
    //recursively calls convertToQuery on children Formula objects
    private BoolFilter handleCompound(Iterator<Formula> formulae, BooleanClauseType clauseType, State state) {

        BoolFilter boolQueryBuilder = queryBuilder.newBoolQuery();

        while (formulae.hasNext()) {
            Formula formula = formulae.next();
            queryBuilder.addClause(boolQueryBuilder, clauseType, (Filter) convertToQuery(formula, state));
        }

        return boolQueryBuilder;
    }

    private List<Filter> reduceLeafClauses(List<Filter> leafClauses, BooleanClauseType booleanClauseType) {
        if (booleanClauseType == BooleanClauseType.MUST) {
            //disabled due to correctness break
            return leafClauses;
        }
        return leafQueryHelper.reduce(leafClauses, LeafQueryHelper.Type.UNION);
    }

    private Filter singleTypeSingleClause(Map<BooleanClauseType, List<Filter>> reducedClauses) {
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
        return null;
    }

    private BoolFilter reduce(BoolFilter boolQueryBuilder) {
        Map<BooleanClauseType, List<Filter>> allClauses = clauseReader.getAllClauses(boolQueryBuilder);
        Map<BooleanClauseType, List<Filter>> reducedClauses = new HashMap<>();
        for (Entry<BooleanClauseType, List<Filter>> entry : allClauses.entrySet()) {
            BooleanClauseType type = entry.getKey();
            List<Filter> clauses = entry.getValue();
            Set<Holder<Filter>> leafClauses = new HashSet<>();
            for (Filter clause : clauses) {
                if (clauseReader.isLeafClause(clause, prefixOn)) {
                    leafClauses.add(new Holder<>(clause, equalsAndHashCodeSupplier));
                } else {
                    SprinklrCollectionUtils.addToMultivaluedMapList(reducedClauses, type, clause);
                }
            }
            if (SprinklrCollectionUtils.isNotEmpty(leafClauses)) {
                List<Filter> reducedLeafClauses = reduceLeafClauses(leafClauses.stream().map(h -> h.obj).collect(Collectors.toList()), type);
                for (Filter reducedLeafClause : reducedLeafClauses) {
                    SprinklrCollectionUtils.addToMultivaluedMapList(reducedClauses, type, reducedLeafClause);
                }
            }
        }
        BoolFilter result = null;
        result = (BoolFilter) singleTypeSingleClause(reducedClauses);
        if (result != null) {
            return result;
        }
        result = queryBuilder.newBoolQuery();
        for (Entry<BooleanClauseType, List<Filter>> entry : reducedClauses.entrySet()) {
            BooleanClauseType type = entry.getKey();
            for (Filter filter : entry.getValue()) {
                queryBuilder.addClause(result, type, filter);
            }
        }
        return result;
    }

    //converts optimized Formula object into
    //Equivalent QueryBuilder form
    private Filter convertToQuery(Formula formula, State<Filter> state) {

        switch (formula.type()) {
            case TRUE: {
                return queryBuilder.newMatchAllQuery();
            }
            case FALSE: {
                return queryBuilder.newMatchNoneQuery();
            }
            case AND: {
                And and = (And) formula;
                return reduce(handleCompound(and.iterator(), BooleanClauseType.MUST, state));
            }
            case OR: {
                Or or = (Or) formula;
                return reduce(handleCompound(or.iterator(), BooleanClauseType.SHOULD, state));
            }
            case NOT: {
                Not not = (Not) formula;
                return reduce(handleCompound(not.iterator(), BooleanClauseType.MUST_NOT, state));
            }
            case LITERAL: {
                Literal literal = (Literal) formula;
                boolean phase = literal.phase();
                Filter queryBuilder1 =  state.getFilter(literal.name());
                if (!phase) {
                    BoolFilter boolQueryBuilder = queryBuilder.newBoolQuery();
                    queryBuilder.addClause(boolQueryBuilder, BooleanClauseType.MUST_NOT, queryBuilder1);
                    return boolQueryBuilder;
                }
                return queryBuilder1;
            }
        }
        throw new UnsupportedOperationException(formula.type() + " is not supported");
    }

    //stores important class-wide entities -
    //HashMaps, FormulaFactory instantiation
    private static class State<Filter> {

        private FormulaFactory formulaFactory = new FormulaFactory();
        private Map<Holder<Filter>, String> variables = new HashMap<>();
        private Map<String, Filter> filterMap = new HashMap<>();
        private final EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier;
        private int varNo = 0;


        public State(EqualsAndHashCodeSupplier<Filter> equalsAndHashCodeSupplier) {
            this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        }

        //stores and retrieves varNames for
        //QueryBuilder objects using Hashmaps
        public Variable getVariable(Filter queryBuilder) {
            Holder<Filter> holder = new Holder<>(queryBuilder, equalsAndHashCodeSupplier);
            String variable = variables.get(holder);
            if (StringUtils.isBlank(variable)) {
                variable = "Var" + ++varNo;
                variables.put(holder, variable);
                filterMap.put(variable, queryBuilder);
            }
            return formulaFactory.variable(variable);
        }

        //retrieves QueryBuilder object
        public Filter getFilter(String variable) {
            return filterMap.get(variable);
        }

    }

    //Holder class for QueryBuilder objects,
    //stores equalsAndHashCodeSupplier (contains equals and hashCode implementation)
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