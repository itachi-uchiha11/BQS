import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.elasticsearch.common.netty.util.internal.StringUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.logicng.formulas.*;
import org.logicng.transformations.simplification.AdvancedSimplifier;
import org.logicng.transformations.simplification.DefaultRatingFunction;

import java.io.IOException;
import java.util.*;

public class BQS<BoolQueryBuilderT extends QueryBuilderT,QueryBuilderT> {
    private final Class<BoolQueryBuilderT> clz;
    private final BooleanClauseReader<BoolQueryBuilderT, QueryBuilderT> clauseReader;
    private final QueryBuilder2<BoolQueryBuilderT, QueryBuilderT> queryBuilder;
    private final EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier;
//    private BooleanClauseReader<BoolQueryBuilder,FilterBuilder> clausereader;

    private BQS(Class<BoolQueryBuilderT> clz, BooleanClauseReader<BoolQueryBuilderT, QueryBuilderT> clauseReader,
               EqualsAndHashCodeSupplier<QueryBuilderT> equalsAndHashCodeSupplier, QueryBuilder2<BoolQueryBuilderT, QueryBuilderT> queryBuilder){
        this.clz = clz;
        this.clauseReader = clauseReader;
        this.equalsAndHashCodeSupplier = equalsAndHashCodeSupplier;
        this.queryBuilder = queryBuilder;
    }
    public static QueryBuilder optimize_boolquerybuilder(BoolQueryBuilder boolquerybuilder){
        EsBoolFilterHelper2 esBoolFilterHelper2 = new EsBoolFilterHelper2();
        return new BQS<>(BoolQueryBuilder.class,esBoolFilterHelper2,esBoolFilterHelper2,esBoolFilterHelper2).optimize(boolquerybuilder);
    }
    private QueryBuilderT optimize(BoolQueryBuilderT boolQueryBuilder){
        State<QueryBuilderT> state = new State<>(equalsAndHashCodeSupplier);
        Formula optimized = convert_to_Formula(boolQueryBuilder,state);
        return convert_to_Query(optimized,state);
    }
    private Formula convert_to_Formula(BoolQueryBuilderT boolQueryBuilder,State state){
        //called recursively at each query node and coalesces them
        //calls reduce_Formula on the aggregate Formula formed
        Map<BooleanClauseType,List<QueryBuilderT>>allclauses = clauseReader.getAllClauses(boolQueryBuilder);
        List<Formula> formulas = new ArrayList<>();
        List<QueryBuilderT> clauses = allclauses.get(BooleanClauseType.MUST);
        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> mustformulae = addClauses(clauses,state);
            Formula formula = simplify_Formula(state.formulaFactory.and(mustformulae));
            formulas.add(formula);
        }
        clauses = allclauses.get(BooleanClauseType.SHOULD);
        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> shouldformulae = addClauses(clauses,state);
            Formula formula = simplify_Formula(state.formulaFactory.or(shouldformulae));
            formulas.add(formula);
        }
        clauses = allclauses.get(BooleanClauseType.MUST_NOT);
        if(SprinklrCollectionUtils.isNotEmpty(clauses)){
            List<Formula> mustnotformulae = addClauses(clauses,state);
            Formula formula = simplify_Formula(state.formulaFactory.not(state.formulaFactory.or(mustnotformulae)));
            formulas.add(formula);
        }
        return state.formulaFactory.and(formulas);
    }

    private List<Formula> addClauses(List<QueryBuilderT> clauses,State state){
        List<Formula> formulas = new ArrayList<>();
        for(QueryBuilderT clause : clauses){
            if(clz.isAssignableFrom(clause.getClass())){//checks for recursive bool query
                formulas.add(convert_to_Formula((BoolQueryBuilderT) clause,state));
            }
            else{
                formulas.add(state.get_variable(clause));
            }
        }
        return formulas;
    }
    private Formula simplify_Formula(Formula formula){
        DefaultRatingFunction defaultRatingFunction = new DefaultRatingFunction();
        AdvancedSimplifier advancedSimplifier = new AdvancedSimplifier(defaultRatingFunction);
        Formula simplified_formula = advancedSimplifier.apply(formula,false);
        return simplified_formula;
    }

    private BoolQueryBuilderT handleCompound(Iterator<Formula> formulae,BooleanClauseType clauseType,State state){
        BoolQueryBuilderT boolQueryBuilder = queryBuilder.newBoolQuery();
        while(formulae.hasNext()){
            Formula formula = formulae.next();
            queryBuilder.addClause(boolQueryBuilder,clauseType,convert_to_Query(formula,state));
        }
        return boolQueryBuilder;
    }
    private QueryBuilderT convert_to_Query(Formula formula,State state){
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
                QueryBuilderT queryBuilder1 = (QueryBuilderT) state.get_filter(literal.name());
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
        public Variable get_variable(QueryBuilderT queryBuilder){
            Holder<QueryBuilderT> holder = new Holder<>(queryBuilder,equalsAndHashCodeSupplier);
            String variable = variableMap.get(holder);
            if(StringUtils.isBlank(variable)){
                variable = "Var" + ++varNo;
                variableMap.put(holder,variable);
                builderMap.put(variable,queryBuilder);
            }
            return formulaFactory.variable(variable);
        }
        public QueryBuilderT get_filter(String variable){
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
    public static void main(String[] args) throws ParserException, IOException {
        BoolQueryBuilder unopt = new BoolQueryBuilder();
        BoolQueryBuilder temp = new BoolQueryBuilder();
        MatchQueryBuilder temp2 = new MatchQueryBuilder("a","India");
        MatchQueryBuilder temp3 = new MatchQueryBuilder("b",1);
        MatchQueryBuilder temp4 = new MatchQueryBuilder("b",2);
        MatchQueryBuilder temp5 = new MatchQueryBuilder("b",3);
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp3));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp4));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp5));
        String x = unopt.toString();
        System.out.println(x);
        QueryBuilder opt = optimize_boolquerybuilder(unopt);
        String x2 = opt.toString();
        System.out.println(x2);

    }
}