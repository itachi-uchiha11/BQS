import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.util.ReflectionUtils;

class EsBoolQueryHelper implements BooleanClauseReader<BoolQueryBuilder, QueryBuilder>, QueryBuilderHelper<BoolQueryBuilder, QueryBuilder>,
        EqualsAndHashCodeSupplier<QueryBuilder>{

    private static final Field mustClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "mustClauses");
    private static final Field shouldClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "shouldClauses");
    private static final Field mustNotClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "mustNotClauses");

    //static block to set field accessible
    static {
            mustClauses.setAccessible(true);
            shouldClauses.setAccessible(true);
            mustNotClauses.setAccessible(true);
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

}
