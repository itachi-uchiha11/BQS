interface QueryBuilder<BoolFilter, Filter> {

    Filter newMatchAllQuery();

    Filter newMatchNoneQuery();

    BoolFilter newBoolQuery();

    void addClause(BoolFilter boolFilter, BooleanClauseType clauseType, Filter filter);
}
