import java.util.List;

interface LeafQueryHelper<Filter> {

    List<Filter> reduce(List<Filter> leafClauses, Type type);

    enum Type {
        INTERSECTION, UNION;
    }

}