package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

/**
 * An iterator iterates over elements of an {@link Iterable}. Its
 * {@link #count()} is the number of remaining elements; when the
 * iterator is flushed out, {@link #count()} is zero, and therefore
 * {@link #isEmpty()} is <code>true</code>.
 */
@DBC
public interface Iterator<G> extends Iterable<G> {

    /**
     * Go to the next element
     */
    @Require("!isEmpty()")
    @Ensure("count() == {old count()} - 1")
    void next();

    @Require("!isEmpty()")
    @Ensure("count() == {old count()}")
    G item();

    @Ensure("isEmpty()")
    <T> Map<G, T> map(final Agent<G, T> mapper);

    @Ensure("isEmpty()")
    <T> T reduce(final Agent<G, T> reducer, final T seed);

    @Ensure("isEmpty()")
    void doAll(final Agent<G, Void> agent);

}
