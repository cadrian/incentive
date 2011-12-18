package net.cadrian.collection;

import net.cadrian.incentive.DBC;

@DBC
public final class MapEntry<K, V> {

    public final K key;
    public final V value;

    MapEntry(final K a_key, final V a_value) {
        this.key = a_key;
        this.value = a_value;
    }

    @Override
    public boolean equals(final Object other) {
        boolean result = false;
        if ((other != null) && (other instanceof MapEntry)) {
            @SuppressWarnings("unchecked")
            final MapEntry<V, K> entry = (MapEntry<V, K>)other;
            result = key.equals(entry.key);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

}
