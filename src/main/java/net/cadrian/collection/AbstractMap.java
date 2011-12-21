package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;

/**
 * A map of elements
 *
 * @author cadrian
 *
 * @param <G>
 *            the type of elements
 */
@DBC
abstract class AbstractMap<K, V> implements WritableMap<K, V> {

    protected int generation;

    @Override
    public int generation() {
        return generation;
    }

    @Override
    public <T> Map<MapEntry<K, V>, T> map(final Agent<MapEntry<K, V>, T> mapper) {
        return iterator().map(mapper);
    }

    @Override
    public <T> T reduce(final Agent<MapEntry<K, V>, T> reducer, final T seed) {
        return iterator().reduce(reducer, seed);
    }

    @Override
    public void doAll(final Agent<MapEntry<K, V>, Void> agent) {
        iterator().doAll(agent);
    }

    @Override
    public Iterator<MapEntry<K, V>> iterator() {
        return new DefaultMapIterator<K, V>(this);
    }

}

@DBC
class DefaultMapIterator<K, V> extends AbstractIterator<MapEntry<K, V>, AbstractMap<K, V>> {

    private int index;

    @Ensure({"iterable == {arg 1}",
            "count() == {arg 1}.count()",
            "index == 0",
            "generation == {arg 1}.generation()"})
    DefaultMapIterator(final AbstractMap<K, V> a_map) {
        super(a_map);
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
    public void next() {
        index++;
    }

    @Override
    public MapEntry<K, V> item() {
        return iterable.item(index);
    }

}
