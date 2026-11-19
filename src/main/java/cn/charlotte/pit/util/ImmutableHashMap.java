package cn.charlotte.pit.util;

import java.util.HashMap;
import java.util.Map;

public class ImmutableHashMap<K, V> extends HashMap<K, V> {

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("ImmutableHashMap");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("ImmutableHashMap");
    }

    @Override
    public V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException("ImmutableHashMap");
    }

    public void forcePut(K key, V value) {
        super.put(key, value);
    }
}
