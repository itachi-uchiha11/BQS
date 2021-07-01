import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.logicng.formulas.*;
import org.logicng.transformations.simplification.AdvancedSimplifier;
import org.logicng.transformations.simplification.DefaultRatingFunction;

import java.util.*;

public class BoolQuerySimplifier<BoolQueryBuilderT extends QueryBuilderT,QueryBuilderT> {
    private final Class<BoolQueryBuilderT> clz;
    private final BooleanClauseReader<BoolQueryBuilderT, QueryBuilderT> clauseReader;
    private final QueryBuilderHelper<BoolQueryBuilderT, QueryBuilderT> queryBuilder;
    private final EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier;
    private final DefaultRatingFunction defaultRatingFunction;
//    private BooleanClauseReader<BoolQueryBuilder,FilterBuilder> clausereader;

    private BoolQuerySimplifier(Class<BoolQueryBuilderT> clz, BooleanClauseReader<BoolQueryBuilderT, QueryBuilderT> clauseReader,
                                EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier, QueryBuilderHelper<BoolQueryBuilderT, QueryBuilderT> queryBuilder,DefaultRatingFunction defaultRatingFunction){
        this.clz = clz;
        this.clauseReader = clauseReader;
        this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        this.queryBuilder = queryBuilder;
        this.defaultRatingFunction = defaultRatingFunction;
    }
    public static QueryBuilder optimizeBoolQueryBuilder(BoolQueryBuilder boolquerybuilder){
        EsBoolQueryHelper esBoolQueryHelper = new EsBoolQueryHelper();
        DefaultRatingFunction defaultRatingFunction = new DefaultRatingFunction();
        return new BoolQuerySimplifier<>(BoolQueryBuilder.class, esBoolQueryHelper, esBoolQueryHelper, esBoolQueryHelper,defaultRatingFunction).optimize(boolquerybuilder);
    }
    public static QueryBuilder optimizeBoolQueryBuilder(BoolQueryBuilder boolquerybuilder,DefaultRatingFunction defaultRatingFunction){
        EsBoolQueryHelper esBoolQueryHelper = new EsBoolQueryHelper();
        return new BoolQuerySimplifier<>(BoolQueryBuilder.class, esBoolQueryHelper, esBoolQueryHelper, esBoolQueryHelper,defaultRatingFunction).optimize(boolquerybuilder);
    }
    private QueryBuilderT optimize(BoolQueryBuilderT boolQueryBuilder){
        State<QueryBuilderT> state = new State<>(equalsAndHashCodeSupplier);
        Formula optimized = convertToFormula(boolQueryBuilder,state);
        return convertToQuery(optimized,state);
    }
    private Formula convertToFormula(BoolQueryBuilderT boolQueryBuilder, State state){
        //called recursively at each query node and coalesces them
        //calls reduce_Formula on the aggregate Formula formed
        Map<BooleanClauseType,List<QueryBuilderT>>allclauses = clauseReader.getAllClauses(boolQueryBuilder);
        List<Formula> formulas = new ArrayList<>();
        List<QueryBuilderT> clauses = allclauses.get(BooleanClauseType.MUST);
        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> mustformulae = addClauses(clauses,state);
            Formula formula = simplifyFormula(state.formulaFactory.and(mustformulae));
            formulas.add(formula);
        }
        clauses = allclauses.get(BooleanClauseType.SHOULD);
        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> shouldformulae = addClauses(clauses,state);
            Formula formula = simplifyFormula(state.formulaFactory.or(shouldformulae));
            formulas.add(formula);
        }
        clauses = allclauses.get(BooleanClauseType.MUST_NOT);
        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> mustnotformulae = addClauses(clauses,state);
            Formula formula = simplifyFormula(state.formulaFactory.not(state.formulaFactory.or(mustnotformulae)));
            formulas.add(formula);
        }
        return state.formulaFactory.and(formulas);
    }

    private List<Formula> addClauses(List<QueryBuilderT> clauses,State state){
        List<Formula> formulas = new ArrayList<>();
        for(QueryBuilderT clause : clauses){
            if(clz.isAssignableFrom(clause.getClass())){//checks for recursive bool query
                formulas.add(convertToFormula((BoolQueryBuilderT) clause,state));
            }
            else{
                formulas.add(state.getVariable(clause));
            }
        }
        return formulas;
    }
    private Formula simplifyFormula(Formula formula){
//        DefaultRatingFunction defaultRatingFunction = new DefaultRatingFunction();
        AdvancedSimplifier advancedSimplifier = new AdvancedSimplifier(defaultRatingFunction);
        Formula simplified_formula = advancedSimplifier.apply(formula,false);
        return simplified_formula;
    }

    private BoolQueryBuilderT handleCompound(Iterator<Formula> formulae,BooleanClauseType clauseType,State state){
        BoolQueryBuilderT boolQueryBuilder = queryBuilder.newBoolQuery();
        while(formulae.hasNext()){
            Formula formula = formulae.next();
            queryBuilder.addClause(boolQueryBuilder,clauseType, convertToQuery(formula,state));
        }
        return boolQueryBuilder;
    }
    private QueryBuilderT convertToQuery(Formula formula, State state){
        switch(formula.type()){
            case TRUE:{
                return queryBuilder.newMatchAllQuery();
            }
            case FALSE:{
                return queryBuilder.newMatchNoneQuery();
            }
            case AND:{
                And and = (And) formula;
                return handleCompound(and.iterator(),BooleanClauseType.MUST,state);
            }
            case OR:{
                Or or = (Or) formula;
                return handleCompound(or.iterator(),BooleanClauseType.SHOULD,state);
            }
            case NOT:{
                Not not = (Not) formula;
                return handleCompound(not.iterator(),BooleanClauseType.MUST_NOT,state);
            }
            case LITERAL:{
                Literal literal = (Literal) formula;
                boolean phase = literal.phase();
                QueryBuilderT queryBuilder1 = (QueryBuilderT) state.getFilter(literal.name());
                if(!phase){
                    BoolQueryBuilderT boolQueryBuilder = queryBuilder.newBoolQuery();
                    queryBuilder.addClause(boolQueryBuilder,BooleanClauseType.MUST_NOT,queryBuilder1);
                    return boolQueryBuilder;
                }
                return queryBuilder1;
            }
        }
        return null;
    }
    private static class State<QueryBuilderT>{
        private FormulaFactory formulaFactory = new FormulaFactory();
        private Map<Holder<QueryBuilderT>,String> variableMap = new HashMap<>();
        private Map<String,QueryBuilderT> builderMap = new HashMap<>();
        private int varNo=0;
        private final EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier;
        public State(EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier) {
            this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        }
        public Variable getVariable(QueryBuilderT queryBuilder){
            Holder<QueryBuilderT> holder = new Holder<>(queryBuilder,equalsAndHashCodeSupplier);
            String variable = variableMap.get(holder);
            if(StringUtils.isBlank(variable)){
                variable = "Var" + ++varNo;
                variableMap.put(holder,variable);
                builderMap.put(variable,queryBuilder);
            }
            return formulaFactory.variable(variable);
        }
        public QueryBuilderT getFilter(String variable){
            return builderMap.get(variable);
        }
    }
    private static class Holder<T> {
        private final T obj;
        private final EqualsAndHashCodeSupplier<T> equalsAndHashCodeSupplier;
        public Holder(T obj,EqualsAndHashCodeSupplier<T>equalsAndHashCodeSupplier) {
            this.obj = obj;
            this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o){return true;}
            if(o == null||getClass()!=o.getClass()){return false;}
            Holder<T> holder = (Holder<T>) o;
            return equalsAndHashCodeSupplier.areEqual(obj, holder.obj);
        }

        @Override
        public int hashCode() {
            return equalsAndHashCodeSupplier.hashCode(obj);
        }
    }

}