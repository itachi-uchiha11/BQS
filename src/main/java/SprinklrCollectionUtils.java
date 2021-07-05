import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SprinklrCollectionUtils {
    public static <K, V> void addToMultivaluedMapList(Map<K, List<V>> map, K key, V value) {
        if (map == null) {
            return;
        }
        if (key == null || value == null) {
            return;
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter,Duplicates
        synchronized (map) {
            List<V> values = map.computeIfAbsent(key, k -> new LinkedList<>());
            values.add(value);
        }
    }
    public static boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }

    public static boolean isNotEmpty(Map map) {
        return !isEmpty(map);
    }
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }

}
