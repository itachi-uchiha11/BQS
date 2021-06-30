import org.elasticsearch.index.query.QueryBuilder;

import java.util.List;
import java.util.Map;

interface BooleanClauseReader<BoolFilterBuilder, FilterBuilder> {

    boolean initialized();

    Map<BooleanClauseType, List<FilterBuilder>> getAllClauses(BoolFilterBuilder boolFilterBuilder);

    boolean isCached(BoolFilterBuilder boolFilterBuilder);

    default List<FilterBuilder> getClauses(BoolFilterBuilder filterBuilder, BooleanClauseType clauseType) {
        return getAllClauses(filterBuilder).get(clauseType);
    }

    boolean isLeafClause(FilterBuilder filterBuilder);

    boolean isMatchAllQuery(FilterBuilder filter);

//    FilterBuilder newMatchAllQuery();
}
