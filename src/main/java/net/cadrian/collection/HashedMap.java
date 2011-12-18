package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;

@DBC
@Invariant("set != null")
public class HashedMap<K, V> extends AbstractCollection<MapEntry<K, V>> implements WritableMap<K, V> {

    private final HashedSet<MapEntry<K, V>> set;

    @Ensure("count() == 0")
    public HashedMap() {
        super();
        set = new HashedSet<MapEntry<K, V>>();
    }

    @Override
    public int count() {
        return set.count();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean has(final K key) {
        return set.has(new MapEntry<K, V>(key, null));
    }

    @Override
    public V at(final K key) {
        return set.at(new MapEntry<K, V>(key, null)).value;
    }

    @Override
    public V ref(final K key) {
        final MapEntry<K, V> entry = set.ref(new MapEntry<K, V>(key, null));
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    @Override
    public K key(final int index) {
        return set.item(index).key;
    }

    @Override
    public V value(final int index) {
        return set.item(index).value;
    }

    @Override
    public MapEntry<K, V> item(final int index) {
        return set.item(index);
    }

    @Override
    public void add(final K key, final V value) {
        set.add(new MapEntry<K, V>(key, value));
        generation++;
    }

    @Override
    public void put(final K key, final V value) {
        set.put(new MapEntry<K, V>(key, value));
        generation++;
    }

    @Override
    public void del(final K key) {
        set.del(new MapEntry<K, V>(key, null));
        generation++;
    }

    @Override
    public MapEntry<K, V>[] toArray(final MapEntry<K, V>[] array) {
        return set.toArray(array);
    }

}
