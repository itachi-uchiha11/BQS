import java.util.List;

interface LeafQueryHelper<QueryBuilder> {

    List<QueryBuilder> reduce(List<QueryBuilder> leafClauses, Type type);

    enum Type {
        AND, OR;
    }

}