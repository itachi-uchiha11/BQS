
import java.util.ArrayList;
import java.util.Arrays;
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

import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.xcontent.XContentType;

class SprBoolFilterHelper implements BooleanClauseReader<SprBoolFilter, SprFilter>, QueryBuilder<SprBoolFilter, SprFilter>,
    EqualsAndHashCodeSupplier<SprFilter>, LeafFilterReducer<SprFilter> {

    private final IdentityHashMap<SprFilter, Integer> queryHashCodeMap = new IdentityHashMap<>();
    private final IdentityHashMap<SprFilter, String> queryMap = new IdentityHashMap<>();

    @Override
    public boolean initialized() {
        return true;
    }

    @Override
    public boolean areEqual(SprFilter f1, SprFilter f2) {
        return Objects.equals(getStringRepresentation(f1), getStringRepresentation(f2));
    }

    @Override
    public int hashCode(SprFilter f1) {
        return queryHashCodeMap.computeIfAbsent(f1, filterBuilder -> getStringRepresentation(filterBuilder).hashCode());
    }

    private String getStringRepresentation(SprFilter filterBuilder) {
        return queryMap.computeIfAbsent(filterBuilder, new Function<SprFilter, String>() {
            @Override
            public String apply(SprFilter filterBuilder) {
                return filterBuilder.toESFilter().buildAsBytes(XContentType.JSON).toUtf8();
            }
        });
    }

    @Override
    public SprFilter newMatchAllQuery() {
        return new SprMatchAllFilterBuilder();
    }

    @Override
    public SprFilter newMatchNoneQuery() {
        return new SprMatchNoneFilterBuilder();
    }

    @Override
    public SprBoolFilter newBoolQuery() {
        return new SprBoolFilterBuilder();
    }

    @Override
    public List<SprFilter> reduce(List<SprFilter> leafClauses, Type type) {
        Map<String, Set<Object>> fieldVsValues = new HashMap<>();
        List<SprFilter> reducedFilters = new ArrayList<>();
        for (SprFilter leafClause : leafClauses) {
            if (leafClause instanceof SprTermFilter) {
                reduceTermFilter(type, fieldVsValues, (SprTermFilter) leafClause);
            } else if (leafClause instanceof SprTermsFilter) {
                if (!reduceTermsFilter(type, fieldVsValues, (SprTermsFilter) leafClause)) {
                    reducedFilters.add(leafClause);
                }
            } else {
                reducedFilters.add(leafClause);
            }
        }
        if (SprinklrCollectionUtils.isNotEmpty(fieldVsValues)) {
            for (Entry<String, Set<Object>> entry : fieldVsValues.entrySet()) {
                if (SprinklrCollectionUtils.isEmpty(entry.getValue())) {
                    reducedFilters.add(0, new SprMatchNoneFilterBuilder());
                } else {
                    reducedFilters.add(new SprTermsFilterBuilder().terms(entry.getKey(), entry.getValue()));
                }
            }
        }
        return reducedFilters;
    }

    @Override
    public void addClause(SprBoolFilter sprBoolFilter, BooleanClauseType clauseType, SprFilter sprFilter) {
        switch (clauseType) {
            case MUST:
                sprBoolFilter.must(sprFilter);
                break;
            case SHOULD:
                sprBoolFilter.should(sprFilter);
                break;
            case MUST_NOT:
                sprBoolFilter.mustNot(sprFilter);
                break;
        }
    }

    @Override
    public Map<BooleanClauseType, List<SprFilter>> getAllClauses(SprBoolFilter sprBoolFilter) {
        Map<BooleanClauseType, List<SprFilter>> result = new HashMap<>();
        List<SprFilter> clauses = sprBoolFilter.getMustFilters();
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST, clauses);
        }
        clauses = sprBoolFilter.getShouldFilters();
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.SHOULD, clauses);
        }
        clauses = sprBoolFilter.getMustNotFilters();
        if (SprinklrCollectionUtils.isNotEmpty(clauses)) {
            result.put(BooleanClauseType.MUST_NOT, clauses);
        }
        return result;
    }

    @Override
    public List<SprFilter> getClauses(SprBoolFilter sprBoolFilter, BooleanClauseType clauseType) {
        switch (clauseType) {
            case MUST:
                return sprBoolFilter.getMustFilters();
            case SHOULD:
                return sprBoolFilter.getShouldFilters();
            case MUST_NOT:
                return sprBoolFilter.getMustNotFilters();
        }
        throw new UnsupportedOperationException("Unsupported type : " + clauseType);
    }

    @Override
    public boolean isCached(SprBoolFilter sprBoolFilter) {
        return sprBoolFilter.cache();
    }

    @Override
    public boolean isLeafClause(SprFilter sprFilter) {
        return sprFilter instanceof SprTermFilter || sprFilter instanceof SprTermsFilter;
    }

    @Override
    public boolean isMatchAllQuery(SprFilter filter) {
        return filter instanceof SprMatchAllFilterBuilder;
    }

    private boolean reduceTermsFilter(Type type, Map<String, Set<Object>> fieldVsValues, SprTermsFilter leafClause) {
        String name = leafClause.getFieldName();
        Object[] value = leafClause.getFieldValue();
        Set<Object> values = fieldVsValues.get(name);
        Set<Object> filterValues = new HashSet<>(Arrays.asList(value));
        if (values == null) {
            values = filterValues;
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

    private void reduceTermFilter(Type type, Map<String, Set<Object>> fieldVsValues, SprTermFilter leafClause) {
        String name = leafClause.getFieldName();
        Object value = leafClause.getFieldValue();
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
}
