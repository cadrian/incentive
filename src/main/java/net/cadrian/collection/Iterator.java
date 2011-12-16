package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

@DBC
public interface Iterator<G> extends Iterable<G> {

    @Require("!isEmpty()")
    void next();

    @Require("!isEmpty()")
    G item();

    @Ensure("isEmpty()")
    <T> Map<G, T> map(final Agent<G, T> mapper);

    @Ensure("isEmpty()")
    <T> T reduce(final Agent<G, T> reducer, final T seed);

    @Ensure("isEmpty()")
    void doAll(final Agent<G, Void> agent);

}
