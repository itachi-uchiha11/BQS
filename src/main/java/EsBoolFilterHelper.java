
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
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchAllFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.springframework.util.ReflectionUtils;

class EsBoolFilterHelper implements BooleanClauseReader<BoolFilterBuilder, FilterBuilder>, QueryBuilder<BoolFilterBuilder, FilterBuilder>,
    EqualsAndHashCodeSupplier<FilterBuilder>, LeafFilterReducer<FilterBuilder> {

    private static final Field mustClauses = ReflectionUtils.findField(BoolFilterBuilder.class, "mustClauses");
    private static final Field shouldClauses = ReflectionUtils.findField(BoolFilterBuilder.class, "shouldClauses");
    private static final Field mustNotClauses = ReflectionUtils.findField(BoolFilterBuilder.class, "mustNotClauses");
    private static final Field termName = ReflectionUtils.findField(TermFilterBuilder.class, "name");
    private static final Field termValue = ReflectionUtils.findField(TermFilterBuilder.class, "value");
    private static final Field termsName = ReflectionUtils.findField(TermsFilterBuilder.class, "name");
    private static final Field termsValues = ReflectionUtils.findField(TermsFilterBuilder.class, "values");
    private static final Field cache = ReflectionUtils.findField(BoolFilterBuilder.class, "cache");
    private static final Field cacheKey = ReflectionUtils.findField(BoolFilterBuilder.class, "cacheKey");
    private static final boolean initialized = mustClauses != null && shouldClauses != null && mustNotClauses != null && cache != null
                                               && termName != null && termValue != null && termsName != null && termsValues != null;

    static {
        if (initialized) {
            mustClauses.setAccessible(true);
            shouldClauses.setAccessible(true);
            mustNotClauses.setAccessible(true);
            cache.setAccessible(true);
            termName.setAccessible(true);
            termValue.setAccessible(true);
            termsName.setAccessible(true);
            termsValues.setAccessible(true);
        }
    }

    private final IdentityHashMap<FilterBuilder, Integer> queryHashCodeMap = new IdentityHashMap<>();
    private final IdentityHashMap<FilterBuilder, String> queryMap = new IdentityHashMap<>();

    @Override
    public boolean initialized() {
        return initialized;
    }

    @Override
    public Map<BooleanClauseType, List<FilterBuilder>> getAllClauses(BoolFilterBuilder boolFilterBuilder) {
        Map<BooleanClauseType, List<FilterBuilder>> result = new HashMap<>();
        List<FilterBuilder> clauses = getConjunctionFilterBuilders(boolFilterBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST, clauses);
        }
        clauses = getDisjunctionFilterBuilders(boolFilterBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.SHOULD, clauses);
        }
        clauses = getNegativeFilterBuilders(boolFilterBuilder);
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST_NOT, clauses);
        }
        return result;
    }

    @Override
    public List<FilterBuilder> getClauses(BoolFilterBuilder boolFilterBuilder, BooleanClauseType clauseType) {
        switch (clauseType) {
            case MUST:
                return getConjunctionFilterBuilders(boolFilterBuilder);
            case SHOULD:
                return getDisjunctionFilterBuilders(boolFilterBuilder);
            case MUST_NOT:
                return getNegativeFilterBuilders(boolFilterBuilder);
        }
        throw new UnsupportedOperationException("Unsupported type : " + clauseType);
    }

    @Override
    public boolean isCached(BoolFilterBuilder boolFilterBuilder) {
        return cache(boolFilterBuilder);
    }

    @Override
    public FilterBuilder newMatchAllQuery() {
        return FilterBuilders.matchAllFilter();
    }

    @Override
    public FilterBuilder newMatchNoneQuery() {
        return FilterBuilders.notFilter(FilterBuilders.matchAllFilter());
    }

    @Override
    public BoolFilterBuilder newBoolQuery() {
        return FilterBuilders.boolFilter();
    }

    @Override
    public boolean isLeafClause(FilterBuilder filterBuilder) {
        return filterBuilder instanceof TermFilterBuilder || filterBuilder instanceof TermsFilterBuilder;
    }

    @Override
    public boolean isMatchAllQuery(FilterBuilder filter) {
        return filter instanceof MatchAllFilterBuilder;
    }

    @Override
    public List<FilterBuilder> reduce(List<FilterBuilder> leafClauses, Type type) {
        Map<String, Set<Object>> fieldVsValues = new HashMap<>();
        List<FilterBuilder> reducedFilters = new ArrayList<>();
        for (FilterBuilder leafClause : leafClauses) {
            if (leafClause instanceof TermFilterBuilder) {
                reduceTermFilter(type, fieldVsValues, leafClause);
            } else if (leafClause instanceof TermsFilterBuilder) {
                if (!reduceTermsFilter(type, fieldVsValues, leafClause)) {
                    reducedFilters.add(leafClause);
                }
            } else {
                reducedFilters.add(leafClause);
            }
        }
        if (SprinklrCollectionUtils.isNotEmpty(fieldVsValues)) {
            for (Entry<String, Set<Object>> entry : fieldVsValues.entrySet()) {
                if (SprinklrCollectionUtils.isEmpty(entry.getValue())) {
                    reducedFilters.add(0, FilterBuilders.notFilter(FilterBuilders.matchAllFilter()));
                } else {
                    reducedFilters.add(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                }
            }
        }
        return reducedFilters;
    }

    @Override
    public void addClause(BoolFilterBuilder boolFilterBuilder, BooleanClauseType clauseType, FilterBuilder filterBuilder) {
        switch (clauseType) {
            case MUST:
                boolFilterBuilder.must(filterBuilder);
                break;
            case SHOULD:
                boolFilterBuilder.should(filterBuilder);
                break;
            case MUST_NOT:
                boolFilterBuilder.mustNot(filterBuilder);
                break;
        }
    }

    @Override
    public boolean areEqual(FilterBuilder f1, FilterBuilder f2) {
        return Objects.equals(getStringRepresentation(f1), getStringRepresentation(f2));
    }

    @Override
    public int hashCode(FilterBuilder f1) {
        return queryHashCodeMap.computeIfAbsent(f1, filterBuilder -> getStringRepresentation(filterBuilder).hashCode());
    }

    private String getStringRepresentation(FilterBuilder filterBuilder) {
        return queryMap.computeIfAbsent(filterBuilder, new Function<FilterBuilder, String>() {
            @Override
            public String apply(FilterBuilder filterBuilder) {
                return filterBuilder.buildAsBytes(XContentType.JSON).toUtf8();
            }
        });
    }


    private boolean reduceTermsFilter(Type type, Map<String, Set<Object>> fieldVsValues, FilterBuilder leafClause) {

        String name = ReflectionUtils.getField(termsName, leafClause).toString();
        Object value = ReflectionUtils.getField(termsValues, leafClause);
        Set<Object> values = fieldVsValues.get(name);
        Set<Object> filterValues = new HashSet<>();
        if (value != null && value.getClass().isArray()) {
            if (value instanceof int[]) {
                Arrays.stream((int[]) value).forEach(filterValues::add);
            } else if (value instanceof long[]) {
                Arrays.stream((long[]) value).forEach(filterValues::add);
            } else if (value instanceof float[]) {
                for (float v : (float[]) value) {
                    filterValues.add(v);
                }
            } else if (value instanceof double[]) {
                Arrays.stream((double[]) value).forEach(filterValues::add);
            } else {
                filterValues.addAll(Arrays.asList((Object[]) value));
            }
        } else if (value instanceof Collection) {
            filterValues.addAll((Collection<?>) value);
        } else if (value instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) value;
            StreamSupport.stream(iterable.spliterator(), false).forEach(filterValues::add);
        } else {
            return false;
        }
        if (values == null) {
            values = new HashSet<>(filterValues);
            fieldVsValues.put(name, values);
        } else {
            switch (type) {
                case UNION:
                    values.addAll(filterValues);
                    break;
                case INTERSECTION:
                    values = Sets.intersection(values, filterValues);
                    fieldVsValues.put(name, values);
                    break;
            }
        }
        return true;
    }

    private void reduceTermFilter(Type type, Map<String, Set<Object>> fieldVsValues, FilterBuilder leafClause) {

        String name = ReflectionUtils.getField(termName, leafClause).toString();
        Object value = ReflectionUtils.getField(termValue, leafClause);
        Set<Object> values = fieldVsValues.get(name);
        if (values == null) {
            values = new HashSet<>();
            values.add(value);
            fieldVsValues.put(name, values);
        } else {
            switch (type) {
                case UNION:
                    values.add(value);
                    break;
                case INTERSECTION:
                    values = Sets.intersection(values, Collections.singleton(value));
                    fieldVsValues.put(name, values);
                    break;
            }
        }
    }

    private static List<FilterBuilder> getDisjunctionFilterBuilders(BoolFilterBuilder boolFilterBuilder) {

        //noinspection unchecked
        return (List<FilterBuilder>) ReflectionUtils.getField(shouldClauses, boolFilterBuilder);
    }

    private static List<FilterBuilder> getConjunctionFilterBuilders(BoolFilterBuilder boolFilterBuilder) {

        //noinspection unchecked
        return (List<FilterBuilder>) ReflectionUtils.getField(mustClauses, boolFilterBuilder);
    }

    private static List<FilterBuilder> getNegativeFilterBuilders(BoolFilterBuilder boolFilterBuilder) {

        //noinspection unchecked
        return (List<FilterBuilder>) ReflectionUtils.getField(mustNotClauses, boolFilterBuilder);
    }

    private static boolean cache(BoolFilterBuilder boolFilterBuilder) {

        return BooleanUtils.isTrue((Boolean) ReflectionUtils.getField(cache, boolFilterBuilder));
    }
}
