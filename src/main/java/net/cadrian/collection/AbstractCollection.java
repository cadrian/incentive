package net.cadrian.collection;

import net.cadrian.incentive.DBC;

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

}
