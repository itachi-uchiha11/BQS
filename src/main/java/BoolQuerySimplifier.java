import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.logicng.formulas.*;
import org.logicng.transformations.simplification.AdvancedSimplifier;
import org.logicng.transformations.simplification.DefaultRatingFunction;
import org.logicng.transformations.simplification.RatingFunction;

import java.util.*;

public class BoolQuerySimplifier<BoolQueryBuilderT extends QueryBuilderT,QueryBuilderT> {

    private final Class<BoolQueryBuilderT> clz;
    private final BooleanClauseReader<BoolQueryBuilderT, QueryBuilderT> clauseReader;
    private final QueryBuilderHelper<BoolQueryBuilderT, QueryBuilderT> queryBuilder;
    private final EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier;
    private final RatingFunction<Integer> ratingFunction;

    private BoolQuerySimplifier(Class<BoolQueryBuilderT> clz, BooleanClauseReader<BoolQueryBuilderT, QueryBuilderT> clauseReader,
                                EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier, QueryBuilderHelper<BoolQueryBuilderT, QueryBuilderT> queryBuilder,RatingFunction ratingFunction){

        this.clz = clz;
        this.clauseReader = clauseReader;
        this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        this.queryBuilder = queryBuilder;
        this.ratingFunction = ratingFunction;

    }

    // 2 different static factory methods
    // based on whether user provides RatingFunction or not
    public static QueryBuilder optimizeBoolQueryBuilder(BoolQueryBuilder boolquerybuilder){

        EsBoolQueryHelper esBoolQueryHelper = new EsBoolQueryHelper();
        DefaultRatingFunction defaultRatingFunction = new DefaultRatingFunction();

        return new BoolQuerySimplifier<>(BoolQueryBuilder.class, esBoolQueryHelper, esBoolQueryHelper, esBoolQueryHelper,defaultRatingFunction).optimize(boolquerybuilder);
    }

    public static QueryBuilder optimizeBoolQueryBuilder(BoolQueryBuilder boolquerybuilder,RatingFunction<Integer> ratingFunction){

        EsBoolQueryHelper esBoolQueryHelper = new EsBoolQueryHelper();

        return new BoolQuerySimplifier<>(BoolQueryBuilder.class, esBoolQueryHelper, esBoolQueryHelper, esBoolQueryHelper,ratingFunction).optimize(boolquerybuilder);
    }

    //Instantiates a new State object
    //calls convertToFormula followed by convertToQuery
    private QueryBuilderT optimize(BoolQueryBuilderT boolQueryBuilder){

        State<QueryBuilderT> state = new State<>(equalsAndHashCodeSupplier);
        Formula optimized = convertToFormula(boolQueryBuilder,state);

        return convertToQuery(optimized,state);
    }

    //Converts the BoolQueryBuilder object into
    //optimized Formula object
    private Formula convertToFormula(BoolQueryBuilderT boolQueryBuilder, State state){

        Map<BooleanClauseType,List<QueryBuilderT>>allclauses = clauseReader.getAllClauses(boolQueryBuilder);
        List<Formula> formulas = new ArrayList<>();
        List<QueryBuilderT> clauses = allclauses.get(BooleanClauseType.MUST);

        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> mustFormulae = addClauses(clauses,state);
            Formula formula = simplifyFormula(state.formulaFactory.and(mustFormulae));
            formulas.add(formula);
        }
        clauses = allclauses.get(BooleanClauseType.SHOULD);

        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> shouldFormulae = addClauses(clauses,state);
            Formula formula = simplifyFormula(state.formulaFactory.or(shouldFormulae));
            formulas.add(formula);
        }
        clauses = allclauses.get(BooleanClauseType.MUST_NOT);

        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> mustNotFormulae = addClauses(clauses,state);
            Formula formula = simplifyFormula(state.formulaFactory.not(state.formulaFactory.or(mustNotFormulae)));
            formulas.add(formula);
        }

        return state.formulaFactory.and(formulas);
    }

    //returns list of Formula objects
    //from List of QueryBuilder objects
    private List<Formula> addClauses(List<QueryBuilderT> clauses,State state){
        List<Formula> formulas = new ArrayList<>();

        for(QueryBuilderT clause : clauses){
            if(clz.isAssignableFrom(clause.getClass())){
                //checks for recursive bool query
                formulas.add(convertToFormula((BoolQueryBuilderT) clause,state));
            }
            else{
                //in case of non bool query, register clause and return Formula object
                formulas.add(state.getVariable(clause));
            }
        }

        return formulas;
    }

    //simplifies Formula object into a reduced equivalent Formula object
    // using AdvancedSimplifier object
    private Formula simplifyFormula(Formula formula){

        AdvancedSimplifier advancedSimplifier = new AdvancedSimplifier(ratingFunction);
        Formula simplified_formula = advancedSimplifier.apply(formula,false);

        return simplified_formula;
    }

    //handles compound Formula objects and
    //recursively calls convertToQuery on children Formula objects
    private BoolQueryBuilderT handleCompound(Iterator<Formula> formulae,BooleanClauseType clauseType,State state){

        BoolQueryBuilderT boolQueryBuilder = queryBuilder.newBoolQuery();

        while(formulae.hasNext()){
            Formula formula = formulae.next();
            queryBuilder.addClause(boolQueryBuilder,clauseType, convertToQuery(formula,state));
        }

        return boolQueryBuilder;
    }

    //converts optimized Formula object into
    //Equivalent QueryBuilder form
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
                QueryBuilderT queryBuilder1 = (QueryBuilderT) state.getBuilder(literal.name());
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

    //stores important class-wide entities -
    //HashMaps, FormulaFactory instantiation
    private static class State<QueryBuilderT>{

        private FormulaFactory formulaFactory = new FormulaFactory();
        private Map<Holder<QueryBuilderT>,String> variableMap = new HashMap<>();
        private Map<String,QueryBuilderT> builderMap = new HashMap<>();
        private final EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier;
        private int varNo=0;


        public State(EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier) {
            this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        }

        //stores and retrieves varNames for
        //QueryBuilder objects using Hashmaps
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

        //retrieves QueryBuilder object
        public QueryBuilderT getBuilder(String variable){
            return builderMap.get(variable);
        }

    }

    //Holder class for QueryBuilder objects,
    //stores equalsAndHashCodeSupplier (contains equals and hashCode implementation)
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