
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.SpanNearQueryBuilder;
import org.elasticsearch.index.query.SpanNotQueryBuilder;
import org.elasticsearch.index.query.SpanQueryBuilder;

/**
 * User: Abhishek Sanoujam
 * Date: 6/19/13
 * Time: 3:02 PM
 */
public class SprBoolFilterBuilder implements SprBoolFilter {

    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(SprBoolFilterBuilder.class);

    private List<SprFilter> mustFilters = Lists.newArrayList();
    private List<SprFilter> shouldFilters = Lists.newArrayList();
    private List<SprFilter> mustNotFilters = Lists.newArrayList();

    private int minimumShouldMatch = 1;

    public SprBoolFilterBuilder() {
    }

    public SprBoolFilter build() {
        return this;
    }

    public boolean cache() {
        return false;
    }

    @Override
    public SprBoolFilterBuilder must(SprFilter sprFilter) {
        mustFilters.add(sprFilter);
        return this;
    }

    @Override
    public SprBoolFilterBuilder should(SprFilter sprFilter) {
        shouldFilters.add(sprFilter);
        return this;
    }

    @Override
    public SprBoolFilterBuilder mustNot(SprFilter sprFilter) {
        mustNotFilters.add(sprFilter);
        return this;
    }


    public int getMinimumShouldMatch() {
        return minimumShouldMatch;
    }

    public void setMinimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
    }

    @Override
    public List<SprFilter> getMustFilters() {
        return mustFilters;
    }

    @Override
    public List<SprFilter> getShouldFilters() {
        return shouldFilters;
    }

    @Override
    public List<SprFilter> getMustNotFilters() {
        return mustNotFilters;
    }

    public void setMustFilters(List<SprFilter> mustFilters) {
        this.mustFilters = mustFilters;
    }

    public void setShouldFilters(List<SprFilter> shouldFilters) {
        this.shouldFilters = shouldFilters;
    }

    public void setMustNotFilters(List<SprFilter> mustNotFilters) {
        this.mustNotFilters = mustNotFilters;
    }

    public SprBoolFilterBuilder must(final SprFilter... mustFilters) {
        for (SprFilter mustFilter : mustFilters) {
            if (mustFilter != null) {
                this.mustFilters.add(mustFilter);
            }
        }
        return this;
    }

    public SprBoolFilterBuilder should(SprFilter... shouldFilters) {
        for (SprFilter shouldFilter : shouldFilters) {
            if (shouldFilter != null) {
                this.shouldFilters.add(shouldFilter);
            }
        }
        return this;
    }

    public SprBoolFilterBuilder mustNot(final SprFilter... mustNotFilters) {
        for (SprFilter mustNotFilter : mustNotFilters) {
            if (mustNotFilter != null) {
                this.mustNotFilters.add(mustNotFilter);
            }
        }
        return this;
    }

    public SprBoolFilterBuilder cache(boolean cache) {
        
        return this;
    }

    public SprBoolFilterBuilder cacheKey(String cacheKey) {
        
        return this;
    }

    @Override
    public Integer getIdentityHash() {
        return null;
    }

    @Override
    public QueryBuilder toQueryBuilder() {
        BoolQueryBuilder queryBuilder = org.elasticsearch.index.query.QueryBuilders.boolQuery();
        for (SprFilter filter : mustFilters) {
            QueryBuilder query = filter.toQueryBuilder();
            if (query != null) {
                queryBuilder.must(query);
            }
        }

        boolean shouldFilterPresent = false;
        for (SprFilter filter : shouldFilters) {
            QueryBuilder query = filter.toQueryBuilder();
            if (query != null) {
                shouldFilterPresent = true;
                queryBuilder.should(query);
            }
        }
        if (shouldFilterPresent) {
            queryBuilder.minimumNumberShouldMatch(minimumShouldMatch);
        }

        for (SprFilter filter : mustNotFilters) {
            QueryBuilder query = filter.toQueryBuilder();
            if (query != null) {
                queryBuilder.mustNot(query);
            }
        }
        return queryBuilder;
    }

    @Override
    public FilterType getType() {
        return null;
    }

    @Override
    public FilterBuilder toESFilter() {

        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();

        for (SprFilter filter : mustFilters) {
            if (filter != null) {
                boolFilter.must(filter.toESFilter());
            }
        }

        for (SprFilter filter : shouldFilters) {
            if (filter != null) {
                boolFilter.should(filter.toESFilter());
            }
        }

        for (SprFilter filter : mustNotFilters) {
            if (filter != null) {
                boolFilter.mustNot(filter.toESFilter());
            }
        }

        
        return boolFilter;
    }

    @Override
    public String toString() {
        return "SprBoolFilterBuilder{" +
                "mustFilters=" + mustFilters +
                ", shouldFilters=" + shouldFilters +
                ", mustNotFilters=" + mustNotFilters +
                '}';
    }

    public boolean anyFilterApplied() {
        return SprinklrCollectionUtils.isNotEmpty(mustNotFilters) || inclusiveFilterExist();
    }

    public boolean inclusiveFilterExist() {
        return mustFilterExist() || shouldFilterExist();
    }

    public boolean shouldFilterExist() {
        return SprinklrCollectionUtils.isNotEmpty(shouldFilters);
    }

    public boolean mustFilterExist() {
        return SprinklrCollectionUtils.isNotEmpty(mustFilters);
    }

    public boolean mustNotFilterExist() {
        return SprinklrCollectionUtils.isNotEmpty(mustNotFilters);
    }

    public boolean hasClauses() {
        return !mustFilters.isEmpty() || !shouldFilters.isEmpty() || !mustNotFilters.isEmpty();
    }
}