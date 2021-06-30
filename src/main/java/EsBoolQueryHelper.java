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
    private static final boolean initialized = (mustClauses != null && shouldClauses != null && mustNotClauses != null);

    static {
        if (initialized) {
            mustClauses.setAccessible(true);
            shouldClauses.setAccessible(true);
            mustNotClauses.setAccessible(true);
        }
    }

    private final IdentityHashMap<QueryBuilder, Integer> queryHashCodeMap = new IdentityHashMap<>();
    private final IdentityHashMap<QueryBuilder, String> queryMap = new IdentityHashMap<>();


    @Override
    public Map<BooleanClauseType, List<QueryBuilder>> getAllClauses(BoolQueryBuilder boolQueryBuilder) {
        Map<BooleanClauseType, List<QueryBuilder>> result = new HashMap<>();
        List<QueryBuilder> clauses = getConjunctionFilterBuilders(boolQueryBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST, clauses);
        }
        clauses = getDisjunctionFilterBuilders(boolQueryBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.SHOULD, clauses);
        }
        clauses = getNegativeFilterBuilders(boolQueryBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST_NOT, clauses);
        }
        return result;
    }


    @Override
    public List<QueryBuilder> getClauses(BoolQueryBuilder boolQueryBuilder, BooleanClauseType clauseType) {
        switch (clauseType) {
            case MUST:
                return getConjunctionFilterBuilders(boolQueryBuilder);
            case SHOULD:
                return getDisjunctionFilterBuilders(boolQueryBuilder);
            case MUST_NOT:
                return getNegativeFilterBuilders(boolQueryBuilder);
        }
        throw new UnsupportedOperationException("Unsupported type : " + clauseType);
    }

    @Override
    public QueryBuilder newMatchAllQuery() {
        return new MatchAllQueryBuilder();
    }

    @Override
    public QueryBuilder newMatchNoneQuery() {
        return new BoolQueryBuilder().mustNot(new MatchAllQueryBuilder());
    }

    @Override
    public BoolQueryBuilder newBoolQuery() {
        return new BoolQueryBuilder();
    }

    @Override
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

    private static List<QueryBuilder> getDisjunctionFilterBuilders(BoolQueryBuilder boolQueryBuilder) {
        //noinspection unchecked
        return (List<QueryBuilder>) ReflectionUtils.getField(shouldClauses, boolQueryBuilder);
    }

    private static List<QueryBuilder> getConjunctionFilterBuilders(BoolQueryBuilder boolQueryBuilder) {
        //noinspection unchecked
        return (List<QueryBuilder>) ReflectionUtils.getField(mustClauses, boolQueryBuilder);
    }

    private static List<QueryBuilder> getNegativeFilterBuilders(BoolQueryBuilder boolQueryBuilder) {
        //noinspection unchecked
        return (List<QueryBuilder>) ReflectionUtils.getField(mustNotClauses, boolQueryBuilder);
    }

}
