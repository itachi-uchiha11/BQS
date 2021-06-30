import java.util.List;
import java.util.Map;

interface BooleanClauseReader<BoolFilterBuilder, FilterBuilder> {

    Map<BooleanClauseType, List<FilterBuilder>> getAllClauses(BoolFilterBuilder boolFilterBuilder);

    default List<FilterBuilder> getClauses(BoolFilterBuilder filterBuilder, BooleanClauseType clauseType) {
        return getAllClauses(filterBuilder).get(clauseType);
    }

}
