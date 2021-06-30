
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;


public interface SprFilter {

    /**
     * Type of filters
     */
    enum FilterType {
        AND, BOOL, OR, RANGE, TERM, TERMS, NOT, IN, REG_EXP, MULTI_MATCH, QUERY_STRING, NESTED, LIMIT, MATCH, MISSING, WILDCARD, CIRCLE, ENVELOPE, PREFIX,
        PREFIX_FILTER, SCRIPT_FILTER, TERM_LOOKUP, EXISTS, FILTER_WRAPPER, SPAN_NEAR, MATCH_ALL, MATCH_NONE, FUZZY, IDS, BOOST
    }

    /**
     * Get the type of the filter
     */
    FilterType getType();


    /**
     * Create elasticsearch equivalent filter
     */
    FilterBuilder toESFilter();

    QueryBuilder toQueryBuilder();



    SprFilter cache(boolean cache);

    SprFilter cacheKey(String cacheKey);

    Integer getIdentityHash();

    default String getEmpty(){
        return "";
    }

    default void setEmpty(String empty){

    }
}
