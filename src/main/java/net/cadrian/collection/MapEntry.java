package net.cadrian.collection;

import net.cadrian.incentive.DBC;

@DBC
public final class MapEntry<K, V> {

    public final K key;
    public final V value;

    MapEntry(K a_key, V a_value) {
        this.key = a_key;
        this.value = a_value;
    }

}
