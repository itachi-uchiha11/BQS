
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsFilterBuilder;

/**
 * User: Abhishek Sanoujam
 * Date: 6/19/13
 * Time: 3:41 PM
 */
public class SprTermsFilterBuilder  implements SprTermsFilter {

    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(SprTermsFilterBuilder.class);

    private String fieldName;
    private String[] nestedFields;
    private Object[] fieldValue;


    public SprTermsFilterBuilder() {
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Object[] getFieldValue() {
        return fieldValue;
    }

    public SprTermsFilterBuilder terms(final String fieldName, Collection fieldValues) {
        return terms(fieldName, fieldValues.toArray());
    }



    public SprTermsFilterBuilder terms(final String fieldName, final String... fieldValue) {
        return terms(fieldName, (Object[]) fieldValue);
    }

    @Override
    public QueryBuilder toQueryBuilder() {
        return QueryBuilders.termsQuery(fieldName, fieldValue);
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
        TermsFilterBuilder rv = FilterBuilders.termsFilter(fieldName, fieldValue);

        return rv;
    }



    @Override
    public String toString() {
        return "SprTermsFilterBuilder{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldValue=" + (fieldValue == null ? null : Arrays.asList(fieldValue)) +
                "} ";
    }



    public SprTermsFilterBuilder terms(final String fieldName, Object... fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        return this;
    }
}
