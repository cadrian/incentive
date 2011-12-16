package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;

/**
 * A collection of elements
 *
 * @author cadrian
 *
 * @param <G>
 *            the type of elements
 */
@DBC
abstract class AbstractCollection<G> implements Collection<G> {

    public <T> Map<G, T> map(final Agent<G, T> mapper) {
        return iterator().map(mapper);
    }

    public <T> T reduce(final Agent<G, T> reducer, final T seed) {
        return iterator().reduce(reducer, seed);
    }

    public void doAll(final Agent<G, Void> agent) {
        iterator().doAll(agent);
    }

    @Override
    public Iterator<G> iterator() {
        return new DefaultCollectionIterator<G>(this);
    }

}

@DBC
class DefaultCollectionIterator<G> extends AbstractIterator<G> {

    private final AbstractCollection<G> collection;
    private int index;

    @Ensure({"collection == {arg 1}",
            "count() == {arg 1}.count()",
            "index == 0"})
    DefaultCollectionIterator(final AbstractCollection<G> a_collection) {
        this.collection = a_collection;
    }

    @Override
    public int count() {
        return collection.count() - index;
    }

    @Override
    public boolean isEmpty() {
        return index == collection.count();
    }

    @Override
    public void next() {
        index++;
    }

    @Override
    public G item() {
        return collection.item(index);
    }

}
