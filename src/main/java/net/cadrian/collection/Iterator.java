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
     * @return <code>true</code> if this Iterator is still useable
     * (i.e. the generation of the spawning Iterable did not change),
     * <code>false</code> otherwise
     */
    boolean isValid();

    /**
     * Go to the next element
     */
    @Require({"isValid()", "!isEmpty()"})
    @Ensure("count() == {old count()} - 1")
    void next();

    @Require({"isValid()", "!isEmpty()"})
    @Ensure("count() == {old count()}")
    G item();

    @Ensure("isEmpty()")
    <T> Map<G, T> map(final Agent<G, T> mapper);

    @Ensure("isEmpty()")
    <T> T reduce(final Agent<G, T> reducer, final T seed);

    @Ensure("isEmpty()")
    void doAll(final Agent<G, Void> agent);

}
