package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Invariant;

@DBC
@Invariant({"keys != null",
        "values != null",
        "orderedKeys != null",
        "keys.length == values.length",
        "orderedKeys.length == keys.length"})
public class HashedMap<K, V> extends AbstractCollection<MapEntry<K, V>> implements WritableMap<K, V> {

    private static final int INDEX_NOT_FOUND = -1;

    private K[] keys;
    private V[] values;
    private int count;

    private K[] orderedKeys;

    public HashedMap() {
        @SuppressWarnings("unchecked")
        final K[] newKeys = (K[])new Object[4];
        @SuppressWarnings("unchecked")
        final K[] newOrderedKeys = (K[])new Object[4];
        @SuppressWarnings("unchecked")
        final V[] newValues = (V[])new Object[4];

        keys = newKeys;
        values = newValues;
        orderedKeys = newOrderedKeys;
    }

    private int _indexOf(final K[] _keys, final K key) {
        // we use Python's method of hashing keys
        int hash = key.hashCode();
        final int initial = hash % _keys.length;
        int result = initial;
        boolean collided = false;
        while (_keys[result] != null && !key.equals(_keys[result]) && result != initial) {
            hash >>= 5;
            result = (5 * result + 1 + hash) % _keys.length;
            collided = true;
        }
        if (collided && result == initial) {
            result = INDEX_NOT_FOUND;
        }
        return result;
    }

    private void grow() {
        @SuppressWarnings("unchecked")
        final K[] newKeys = (K[])new Object[keys.length*2];
        @SuppressWarnings("unchecked")
        final K[] newOrderedKeys = (K[])new Object[4];
        @SuppressWarnings("unchecked")
        final V[] newValues = (V[])new Object[values.length*2];

        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                final int index = _indexOf(newKeys, keys[i]);
                newKeys[index] = keys[i];
                newValues[index] = values[i];
            }
        }

        System.arraycopy(orderedKeys, 0, newOrderedKeys, 0, count);

        keys = newKeys;
        values = newValues;
        orderedKeys = newOrderedKeys;
    }

    private int indexOf(final K key) {
        int result = _indexOf(keys, key);
        while (result == INDEX_NOT_FOUND) {
            grow();
            result = _indexOf(keys, key);
        }
        return result;
    }

    public int count() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean has(final K key) {
        return _indexOf(keys, key) != INDEX_NOT_FOUND;
    }

    public V at(final K key) {
        return values[_indexOf(keys, key)];
    }

    public V ref(final K key) {
        final int index = _indexOf(keys, key);
        if (index == INDEX_NOT_FOUND) return null;
        return values[index];
    }

    public K key(final int index) {
        return orderedKeys[index];
    }

    public V value(final int index) {
        return at(orderedKeys[index]);
    }

    public MapEntry<K, V> item(final int index) {
        final K k = orderedKeys[index];
        return new MapEntry<K, V>(k, at(k));
    }

    public void add(final K key, final V value) {
        // TODO
    }

    public void put(final K key, final V value) {
        // TODO
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
