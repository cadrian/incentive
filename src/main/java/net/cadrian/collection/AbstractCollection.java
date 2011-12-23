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

    protected int generation;

    @Override
    public int generation() {
        return generation;
    }

    @Override
    public <T> Map<G, T> map(final Agent<G, T> mapper) {
        return iterator().map(mapper);
    }

    @Override
    public <T> T reduce(final Agent<G, T> reducer, final T seed) {
        return iterator().reduce(reducer, seed);
    }

    @Override
    public void doAll(final Agent<G, Void> agent) {
        iterator().doAll(agent);
    }

    @Override
    public Iterator<G> iterator() {
        return new DefaultCollectionIterator<G>(this);
    }

}
