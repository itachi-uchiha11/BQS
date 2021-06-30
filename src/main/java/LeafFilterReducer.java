import java.util.List;

interface LeafFilterReducer<Filter> {

    List<Filter> reduce(List<Filter> leafClauses, Type type);

    enum Type {
        INTERSECTION, UNION;
    }

}
