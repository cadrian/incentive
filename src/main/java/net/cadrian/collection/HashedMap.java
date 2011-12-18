package net.cadrian.collection;

import java.lang.reflect.Array;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;

@DBC
@Invariant({"keys != null",
        "values != null",
        "used != null",
        "keys.length == values.length",
        "keys.length == used.length"})
public class HashedMap<K, V> extends AbstractCollection<MapEntry<K, V>> implements WritableMap<K, V> {

    private static final int MAP_FULL = -1;

    static class IterationCache {
        int externalIndex;
        int keyIndex;

        void update(final int ext, final int key) {
            externalIndex = ext;
            keyIndex = key;
        }
    }

    private K[] keys;
    private boolean[] used;
    private V[] values;
    private int count;

    private final IterationCache indexCache;

    @Ensure("count() == 0")
    public HashedMap() {
        @SuppressWarnings("unchecked")
        final K[] newKeys = (K[])new Object[4];
        final boolean[] newUsed = new boolean[4];
        @SuppressWarnings("unchecked")
        final V[] newValues = (V[])new Object[4];

        keys = newKeys;
        used = newUsed;
        values = newValues;

        indexCache = new IterationCache();
    }

    private int _indexOf(final K[] _keys, final boolean[] _used, final K key) {
        // we use Python's method of hashing keys
        int hash = key.hashCode();
        final int mask = _keys.length - 1;
        final int initial = hash & mask;
        int result = initial;
        while ((_used == null ? _keys[result] != null : _used[result]) && (_keys[result] == null || !_keys[result].equals(key))) {
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
        final boolean[] newUsed = new boolean[newCapacity];
        @SuppressWarnings("unchecked")
        final V[] newValues = (V[])new Object[newCapacity];

        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                final int index = _indexOf(newKeys, null, keys[i]);
                newKeys[index] = keys[i];
                newValues[index] = values[i];
                newUsed[index] = true;
            }
        }

        keys = newKeys;
        used = newUsed;
        values = newValues;
    }

    private int indexOf(final K key, final boolean followUsed) {
        int result = _indexOf(keys, followUsed ? used : null, key);
        while (result == MAP_FULL) {
            grow();
            result = _indexOf(keys, followUsed ? used : null, key);
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
        final int index = _indexOf(keys, used, key);
        return index != MAP_FULL && keys[index] != null;
    }

    @Override
    public V at(final K key) {
        return values[_indexOf(keys, used, key)];
    }

    @Override
    public V ref(final K key) {
        final int index = _indexOf(keys, used, key);
        if (index == MAP_FULL || keys[index] == null) {
            return null;
        }
        return values[index];
    }

    private void findIndex(final IterationCache cache, final int index) {
        if (index != cache.externalIndex || keys[cache.keyIndex] == null) {
            int externalIndex = cache.externalIndex;
            int keyIndex = cache.keyIndex;

            if (externalIndex > index) {
                externalIndex = 0;
                keyIndex = 0;
            }
            else {
                keyIndex++;
            }

            while (externalIndex < index || keys[keyIndex] == null) {
                if (keys[keyIndex] == null) {
                    keyIndex++;
                }
                else {
                    externalIndex++;
                }
            }

            cache.update(externalIndex, keyIndex);
        }
    }

    K cachedKey(final int index, final IterationCache cache) {
        findIndex(cache, index);
        return keys[cache.keyIndex];
    }

    V cachedValue(final int index, final IterationCache cache) {
        findIndex(cache, index);
        return values[cache.keyIndex];
    }

    MapEntry<K, V> cachedItem(final int index, final IterationCache cache) {
        findIndex(cache, index);
        return new MapEntry<K, V>(keys[cache.keyIndex], values[cache.keyIndex]);
    }

    @Override
    public K key(final int index) {
        return cachedKey(index, indexCache);
    }

    @Override
    public V value(final int index) {
        return cachedValue(index, indexCache);
    }

    @Override
    public MapEntry<K, V> item(final int index) {
        return cachedItem(index, indexCache);
    }

    @Override
    public void add(final K key, final V value) {
        final int index = indexOf(key, false);
        keys[index] = key;
        used[index] = true;
        values[index] = value;
        count++;
        generation++;
    }

    @Override
    public void put(final K key, final V value) {
        int index = indexOf(key, true);
        if (keys[index] == null) {
            index = indexOf(key, false);
            count++;
            keys[index] = key;
        }
        used[index] = true;
        values[index] = value;
        generation++;
    }

    @Override
    public void del(final K key) {
        final int index = indexOf(key, true);
        keys[index] = null;
        values[index] = null;
        // but used[index] stays true to enable to follow collisions
        count--;
        generation++;
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
        return new HashedMapIterator<K, V>(this, new IterationCache());
    }

}

@DBC
class HashedMapIterator<K, V> extends AbstractIterator<MapEntry<K, V>, HashedMap<K, V>> {

    private final HashedMap.IterationCache cache;
    private int index;

    HashedMapIterator(HashedMap<K, V> map, HashedMap.IterationCache cache) {
        super(map);
        this.cache = cache;
        index = 0;
    }

    @Override
    public int count() {
        return iterable.count() - index;
    }

    @Override
    public boolean isEmpty() {
        return index == iterable.count();
    }

    @Override
    public MapEntry<K, V> item() {
        return iterable.cachedItem(index, cache);
    }

    @Override
    public void next() {
        index++;
    }

}
