
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * @author rahulanishetty
 * @since 25/04/17.
 */
public class SprMatchNoneFilterBuilder implements SprFilter {

    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(SprMatchNoneFilterBuilder.class);

    public SprMatchNoneFilterBuilder() {
    }

    @Override
    public QueryBuilder toQueryBuilder() {
        return QueryBuilders.constantScoreQuery(FilterBuilders.boolFilter().mustNot(FilterBuilders.matchAllFilter()));
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
        return FilterBuilders.boolFilter().mustNot(FilterBuilders.matchAllFilter());
    }

    @Override
    public String toString() {
        return "SprMatchNoneFilterBuilder{} ";
    }
}
