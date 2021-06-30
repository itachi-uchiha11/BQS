
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.util.ReflectionUtils;

class EsBoolQueryHelper implements BooleanClauseReader<BoolQueryBuilder, QueryBuilder>, QueryBuilder2<BoolQueryBuilder, QueryBuilder>,
        EqualsAndHashCodeSupplier<QueryBuilder>{

    private static final Field mustClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "mustClauses");
    private static final Field shouldClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "shouldClauses");
    private static final Field mustNotClauses = ReflectionUtils.findField(BoolQueryBuilder.class, "mustNotClauses");
//    private static final Field termName = ReflectionUtils.findField(TermFilterBuilder.class, "name");
//    private static final Field termValue = ReflectionUtils.findField(TermFilterBuilder.class, "value");
//    private static final Field termsName = ReflectionUtils.findField(TermsFilterBuilder.class, "name");
//    private static final Field termsValues = ReflectionUtils.findField(TermsFilterBuilder.class, "values");
//    private static final Field cache = ReflectionUtils.findField(BoolFilterBuilder.class, "cache");
//    private static final Field cacheKey = ReflectionUtils.findField(BoolFilterBuilder.class, "cacheKey");
    private static final boolean initialized = (mustClauses != null && shouldClauses != null && mustNotClauses != null); //&& cache != null
            //&& termName != null && termValue != null && termsName != null && termsValues != null;

    static {
        if (initialized) {
            mustClauses.setAccessible(true);
            shouldClauses.setAccessible(true);
            mustNotClauses.setAccessible(true);
//            cache.setAccessible(true);
//            termName.setAccessible(true);
//            termValue.setAccessible(true);
//            termsName.setAccessible(true);
//            termsValues.setAccessible(true);
        }
    }

    private final IdentityHashMap<QueryBuilder, Integer> queryHashCodeMap = new IdentityHashMap<>();
    private final IdentityHashMap<QueryBuilder, String> queryMap = new IdentityHashMap<>();

    @Override
    public boolean initialized() {
        return initialized;
    }

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
    public boolean isCached(BoolQueryBuilder boolQueryBuilder) {
        return false;
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
    public boolean isLeafClause(QueryBuilder queryBuilder) {
        return false;
    }

    @Override
    public boolean isMatchAllQuery(QueryBuilder filter) {
        return false;
    }

//    @Override
//    public boolean isCached(BoolFilterBuilder boolFilterBuilder) {
//        return cache(boolFilterBuilder);
//    }

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

//    @Override
//    public boolean isLeafClause(FilterBuilder filterBuilder) {
//        return filterBuilder instanceof TermFilterBuilder || filterBuilder instanceof TermsFilterBuilder;
//    }
//
//    @Override
//    public boolean isMatchAllQuery(FilterBuilder filter) {
//        return filter instanceof MatchAllFilterBuilder;
//    }
//
//    @Override
//    public List<FilterBuilder> reduce(List<FilterBuilder> leafClauses, Type type) {
//        Map<String, Set<Object>> fieldVsValues = new HashMap<>();
//        List<FilterBuilder> reducedFilters = new ArrayList<>();
//        for (FilterBuilder leafClause : leafClauses) {
//            if (leafClause instanceof TermFilterBuilder) {
//                reduceTermFilter(type, fieldVsValues, leafClause);
//            } else if (leafClause instanceof TermsFilterBuilder) {
//                if (!reduceTermsFilter(type, fieldVsValues, leafClause)) {
//                    reducedFilters.add(leafClause);
//                }
//            } else {
//                reducedFilters.add(leafClause);
//            }
//        }
//        if (SprinklrCollectionUtils.isNotEmpty(fieldVsValues)) {
//            for (Entry<String, Set<Object>> entry : fieldVsValues.entrySet()) {
//                if (SprinklrCollectionUtils.isEmpty(entry.getValue())) {
//                    reducedFilters.add(0, FilterBuilders.notFilter(FilterBuilders.matchAllFilter()));
//                } else {
//                    reducedFilters.add(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
//                }
//            }
//        }
//        return reducedFilters;
//    }

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


//    private boolean reduceTermsFilter(Type type, Map<String, Set<Object>> fieldVsValues, FilterBuilder leafClause) {
//
//        String name = ReflectionUtils.getField(termsName, leafClause).toString();
//        Object value = ReflectionUtils.getField(termsValues, leafClause);
//        Set<Object> values = fieldVsValues.get(name);
//        Set<Object> filterValues = new HashSet<>();
//        if (value != null && value.getClass().isArray()) {
//            if (value instanceof int[]) {
//                Arrays.stream((int[]) value).forEach(filterValues::add);
//            } else if (value instanceof long[]) {
//                Arrays.stream((long[]) value).forEach(filterValues::add);
//            } else if (value instanceof float[]) {
//                for (float v : (float[]) value) {
//                    filterValues.add(v);
//                }
//            } else if (value instanceof double[]) {
//                Arrays.stream((double[]) value).forEach(filterValues::add);
//            } else {
//                filterValues.addAll(Arrays.asList((Object[]) value));
//            }
//        } else if (value instanceof Collection) {
//            filterValues.addAll((Collection<?>) value);
//        } else if (value instanceof Iterable) {
//            Iterable<?> iterable = (Iterable<?>) value;
//            StreamSupport.stream(iterable.spliterator(), false).forEach(filterValues::add);
//        } else {
//            return false;
//        }
//        if (values == null) {
//            values = new HashSet<>(filterValues);
//            fieldVsValues.put(name, values);
//        } else {
//            switch (type) {
//                case UNION:
//                    values.addAll(filterValues);
//                    break;
//                case INTERSECTION:
//                    values = Sets.intersection(values, filterValues);
//                    fieldVsValues.put(name, values);
//                    break;
//            }
//        }
//        return true;
//    }
//
//    private void reduceTermFilter(Type type, Map<String, Set<Object>> fieldVsValues, FilterBuilder leafClause) {
//
//        String name = ReflectionUtils.getField(termName, leafClause).toString();
//        Object value = ReflectionUtils.getField(termValue, leafClause);
//        Set<Object> values = fieldVsValues.get(name);
//        if (values == null) {
//            values = new HashSet<>();
//            values.add(value);
//            fieldVsValues.put(name, values);
//        } else {
//            switch (type) {
//                case UNION:
//                    values.add(value);
//                    break;
//                case INTERSECTION:
//                    values = Sets.intersection(values, Collections.singleton(value));
//                    fieldVsValues.put(name, values);
//                    break;
//            }
//        }
//    }

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

//    private static boolean cache(BoolQueryBuilder boolQueryBuilder) {
//
//        return BooleanUtils.isTrue((Boolean) ReflectionUtils.getField(cache, boolQueryBuilder));
//    }
}
