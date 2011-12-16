package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Invariant;

@DBC
@Invariant({"keys != null", "values != null"})
public class HashedMap<K, V> extends AbstractCollection<MapEntry<K, V>> implements WritableMap<K, V> {

    private K[] keys;
    private V[] values;
    private int count;

    public HashedMap() {
        @SuppressWarnings("unchecked")
        final K[] newKeys = (K[])new Object[4];
        @SuppressWarnings("unchecked")
        final V[] newValues = (V[])new Object[4];

        keys = newKeys;
        values = newValues;
    }

    public int count() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean has(final K key) {
        // TODO
        return false;
    }

    public V at(final K key) {
        // TODO
        return null;
    }

    public K key(final int index) {
        // TODO
        return null;
    }

    public V value(final int index) {
        // TODO
        return null;
    }

    public MapEntry<K, V> item(final int index) {
        // TODO
        return null;
    }

    public void add(final K key, final V value) {
    }

    public void put(final K key, final V value) {
    }

    public MapEntry<K, V>[] toArray(final MapEntry<K, V>[] array) {
        // TODO
        return null;
    }

    public Iterator<MapEntry<K, V>> iterator() {
        // TODO
        return null;
    }

}
