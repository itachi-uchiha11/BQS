
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * @author rahulanishetty
 * @since 25/04/17.
 */
public class SprMatchAllFilterBuilder implements SprFilter{

    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(SprMatchAllFilterBuilder.class);

    public SprMatchAllFilterBuilder() {
    }


    @Override
    public QueryBuilder toQueryBuilder() {
        return QueryBuilders.matchAllQuery();
    }

    @Override
    public SprFilter cache(boolean cache) {
        return null;
    }

    @Override
    public SprFilter cacheKey(String cacheKey) {
        return null;
    }

    @Override
    public Integer getIdentityHash() {
        return null;
    }

    @Override
    public FilterType getType() {
        return null;
    }

    @Override
    public FilterBuilder toESFilter() {
        return FilterBuilders.matchAllFilter();
    }

    @Override
    public String toString() {
        return "SprMatchAllFilterBuilder{} ";
    }
}

