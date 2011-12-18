package net.cadrian.collection;

import java.lang.reflect.Array;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;

@DBC
@Invariant({"keys != null",
        "values != null",
        "orderedKeys != null",
        "keys.length == values.length",
        "orderedKeys.length == keys.length"})
public class HashedMap<K, V> extends AbstractCollection<MapEntry<K, V>> implements WritableMap<K, V> {

    private static final int MAP_FULL = -1;

    private K[] keys;
    private V[] values;
    private int count;

    private K[] orderedKeys;

    @Ensure("count() == 0")
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
        final int mask = _keys.length - 1;
        final int initial = hash & mask;
        int result = initial;
        while (_keys[result] != null && !_keys[result].equals(key)) {
            hash >>= 5;
            result = (5 * result + 1 + hash) & mask;
            if (result == initial) {
                result = MAP_FULL;
                break;
            }
        }

        return result;
    }

    private void grow() {
        final int newCapacity = keys.length * 2;

        @SuppressWarnings("unchecked")
        final K[] newKeys = (K[])new Object[newCapacity];
        @SuppressWarnings("unchecked")
        final K[] newOrderedKeys = (K[])new Object[newCapacity];
        @SuppressWarnings("unchecked")
        final V[] newValues = (V[])new Object[newCapacity];

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
        while (result == MAP_FULL) {
            grow();
            result = _indexOf(keys, key);
        }
        return result;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public boolean has(final K key) {
        final int index = _indexOf(keys, key);
        return index != MAP_FULL && keys[index] != null;
    }

    @Override
    public V at(final K key) {
        return values[_indexOf(keys, key)];
    }

    @Override
    public V ref(final K key) {
        final int index = _indexOf(keys, key);
        if (index == MAP_FULL || keys[index] == null) {
            return null;
        }
        return values[index];
    }

    @Override
    public K key(final int index) {
        return orderedKeys[index];
    }

    @Override
    public V value(final int index) {
        return at(orderedKeys[index]);
    }

    @Override
    public MapEntry<K, V> item(final int index) {
        final K key = orderedKeys[index];
        return new MapEntry<K, V>(key, at(key));
    }

    @Override
    public void add(final K key, final V value) {
        final int index = indexOf(key);
        keys[index] = key;
        values[index] = value;
        orderedKeys[count++] = key;
    }

    @Override
    public void put(final K key, final V value) {
        final int index = indexOf(key);
        if (keys[index] == null) {
            keys[index] = key;
            orderedKeys[count++] = key;
        }
        values[index] = value;
    }

    @Override
    public MapEntry<K, V>[] toArray(final MapEntry<K, V>[] array) {
        final MapEntry<K, V>[] result;
        if (array == null) {
            @SuppressWarnings("unchecked")
            final MapEntry<K, V>[] newArray = (MapEntry<K, V>[])new Object[count()];
            result = newArray;
        }
        else if (array.length < count()) {
            @SuppressWarnings("unchecked")
            final MapEntry<K, V>[] newArray = (MapEntry<K, V>[])Array.newInstance(array.getClass().getComponentType(), count());
            result = newArray;
        }
        else {
            result = array;
        }
        for (int i = 0; i < count; i++) {
            result[i] = item(i);
        }
        return result;
    }

    @Override
    public Iterator<MapEntry<K, V>> iterator() {
        // TODO
        return null;
    }

}
