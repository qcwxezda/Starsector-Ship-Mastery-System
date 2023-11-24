package shipmastery.util;

import java.util.LinkedHashMap;
import java.util.Map;
public class SizeLimitedMap<K, V> extends LinkedHashMap<K, V> {
    final int maxSize;

    public SizeLimitedMap(int size) {
        maxSize = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
