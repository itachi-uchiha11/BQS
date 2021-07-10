import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.util.ReflectionUtils;

class EsBoolQueryHelper implements BooleanClauseReader<BoolQueryBuilder, QueryBuilder>, QueryBuilderHelper<BoolQueryBuilder, QueryBuilder>,
        EqualsAndHashCodeSupplier<QueryBuilder>, LeafQueryHelper<QueryBuilder>{

    private static final Field mustClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "mustClauses");
    private static final Field shouldClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "shouldClauses");
    private static final Field mustNotClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "mustNotClauses");
    private static final Field termName = ReflectionUtils.findField(TermQueryBuilder.class, "name");
    private static final Field termValue = ReflectionUtils.findField(TermQueryBuilder.class, "value");
    private static final Field termsName = ReflectionUtils.findField(TermsQueryBuilder.class, "name");
    private static final Field termsValue = ReflectionUtils.findField(TermsQueryBuilder.class, "values");


    //static block to set field accessible
    static {
        mustClauses.setAccessible(true);
        shouldClauses.setAccessible(true);
        mustNotClauses.setAccessible(true);
        termName.setAccessible(true);
        termValue.setAccessible(true);
        termsName.setAccessible(true);
        termsValue.setAccessible(true);
    }

    private final IdentityHashMap<QueryBuilder, Integer> queryHashCodeMap = new IdentityHashMap<>();
    private final IdentityHashMap<QueryBuilder, String> queryMap = new IdentityHashMap<>();


    @Override
    //returns all member clauses grouped by ClauseType
    public Map<BooleanClauseType, List<QueryBuilder>> getAllClauses(BoolQueryBuilder boolQueryBuilder) {

        Map<BooleanClauseType, List<QueryBuilder>> result = new HashMap<>();

        List<QueryBuilder> clauses = getConjunctionQueryBuilders(boolQueryBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST, clauses);
        }

        clauses = getDisjunctionQueryBuilders(boolQueryBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.SHOULD, clauses);
        }

        clauses = getNegativeQueryBuilders(boolQueryBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST_NOT, clauses);
        }

        return result;
    }


    @Override
    //returns clauses of specific ClauseType
    public List<QueryBuilder> getClauses(BoolQueryBuilder boolQueryBuilder, BooleanClauseType clauseType) {

        switch (clauseType) {
            case MUST:
                return getConjunctionQueryBuilders(boolQueryBuilder);
            case SHOULD:
                return getDisjunctionQueryBuilders(boolQueryBuilder);
            case MUST_NOT:
                return getNegativeQueryBuilders(boolQueryBuilder);
        }

        throw new UnsupportedOperationException("Unsupported type : " + clauseType);
    }

    @Override
    public boolean isLeafClause(QueryBuilder clause) {
        return (clause instanceof TermsQueryBuilder || clause instanceof TermQueryBuilder);
    }

    @Override
    //QueryBuilder corresponding to True boolean expression
    public QueryBuilder newMatchAllQuery() {
        return new MatchAllQueryBuilder();
    }

    @Override
    //QueryBuilder corresponding to False boolean expression
    public QueryBuilder newMatchNoneQuery() {
        return new BoolQueryBuilder().mustNot(new MatchAllQueryBuilder());
    }

    @Override
    //returns new BoolQueryBuilder object
    public BoolQueryBuilder newBoolQuery() {
        return new BoolQueryBuilder();
    }

    @Override
    //adds clauses in specified ClauseType field
    public void addClause(BoolQueryBuilder boolQueryBuilder, BooleanClauseType clauseType, QueryBuilder queryBuilder) {

        switch (clauseType) {
            case MUST:
                boolQueryBuilder.must(queryBuilder);
                break;
            case SHOULD:
                boolQueryBuilder.should(queryBuilder);
                break;
            case MUST_NOT:
                boolQueryBuilder.mustNot(queryBuilder);
                break;
        }

    }

    @Override
    public boolean areEqual(QueryBuilder f1, QueryBuilder f2) {
        return Objects.equals(getStringRepresentation(f1), getStringRepresentation(f2));
    }

    @Override
    public int hashCode(QueryBuilder f1) {
        return queryHashCodeMap.computeIfAbsent(f1, queryBuilder -> getStringRepresentation(queryBuilder).hashCode());
    }

    private String getStringRepresentation(QueryBuilder queryBuilder) {
        return queryMap.computeIfAbsent(queryBuilder, new Function<QueryBuilder, String>() {
            @Override
            public String apply(QueryBuilder queryBuilder) {
                return queryBuilder.buildAsBytes(XContentType.JSON).toUtf8();
            }
        });
    }

    //returns list of Should member clauses
    private static List<QueryBuilder> getDisjunctionQueryBuilders(BoolQueryBuilder boolQueryBuilder) {
        return (List<QueryBuilder>) ReflectionUtils.getField(shouldClauses, boolQueryBuilder);
    }

    //returns list of Must member clauses
    private static List<QueryBuilder> getConjunctionQueryBuilders(BoolQueryBuilder boolQueryBuilder) {
        return (List<QueryBuilder>) ReflectionUtils.getField(mustClauses, boolQueryBuilder);
    }

    //returns list of MustNot member clauses
    private static List<QueryBuilder> getNegativeQueryBuilders(BoolQueryBuilder boolQueryBuilder) {
        return (List<QueryBuilder>) ReflectionUtils.getField(mustNotClauses, boolQueryBuilder);
    }

    private boolean reduceTermsQueryBuilder(QueryBuilder queryBuilder,Type type,Map<String,Set<Object>> fieldAndValues){
        String name = ReflectionUtils.getField(termsName,queryBuilder).toString();
        Object value = ReflectionUtils.getField(termsValue,queryBuilder);
        Set<Object>values = fieldAndValues.get(name);
        Set<Object>filteredValues = new HashSet<>();
        if(value == null){
            return false;
        }
        if(value.getClass().isArray()){
            if(value instanceof int[]){
                Arrays.stream((int[]) value).forEach(filteredValues::add);
            }
            else if(value instanceof double[]){
                Arrays.stream((double[]) value).forEach(filteredValues::add);
            }
            else if(value instanceof long[]){
                Arrays.stream((long[]) value).forEach(filteredValues::add);
            }
            else if(value instanceof float[]){
                for(float f:(float [])value){
                    filteredValues.add(f);
                }
            }
            else {
                filteredValues.addAll(Arrays.asList((Object[]) value));
            }
        }
        else if(value instanceof Collection){
            filteredValues.addAll((Collection<?>) value);
        }
        else if(value instanceof Iterable){
            Iterable<?> iterable = (Iterable<?>) value;
            StreamSupport.stream(iterable.spliterator(), false).forEach(filteredValues::add);
        }
        else{
            return false;
        }

        if(values == null){
            values = new HashSet<>(filteredValues);
            fieldAndValues.put(name,values);
        }
        else{
            switch(type){
                case AND:{
                    //dsabled
                    break;
                }
                case OR:{
                    values.addAll(filteredValues);
                    break;
                }
            }
        }
        return true;
    }

    private void reduceTermQueryBuilder(QueryBuilder queryBuilder,Type type,Map<String,Set<Object>> fieldAndValues){
        String name = ReflectionUtils.getField(termName,queryBuilder).toString();
        Object value = ReflectionUtils.getField(termValue,queryBuilder);
        Set<Object>values = fieldAndValues.get(name);
        if(values == null){
            values = new HashSet<>();
            values.add(value);
            fieldAndValues.put(name,values);
        }
        else{
            switch(type){
                case AND:{
                    //dsabled
                    break;
                }
                case OR:{
                    values.add(value);
                    break;
                }
            }
        }
    }

    @Override
    public List<QueryBuilder> reduce(List<QueryBuilder> leafClauses, Type type) {
        Map<String, Set<Object>>fieldAndValues = new HashMap<>();
        List<QueryBuilder> reducedBuilders = new ArrayList<>();
        for(QueryBuilder leaf : leafClauses){
            if(leaf instanceof TermQueryBuilder){
                reduceTermQueryBuilder(leaf,type,fieldAndValues);
            }
            else if(leaf instanceof TermsQueryBuilder && reduceTermsQueryBuilder(leaf,type,fieldAndValues)){

            }
            else{
                reducedBuilders.add(leaf);
            }
        }
        for(Entry<String,Set<Object>> entry : fieldAndValues.entrySet()){
            if(SprinklrCollectionUtils.isEmpty(entry.getValue())){
                reducedBuilders.add(0,newMatchNoneQuery());
            }
            else{
                reducedBuilders.add(new TermsQueryBuilder(entry.getKey(),entry.getValue()));
            }
        }
        return  reducedBuilders;
    }
}
