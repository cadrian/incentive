package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

@DBC
public interface WritableSet<G> extends Set<G> {

    /**
     * Put the given element. The element may already be
     * present.
     *
     * @param element the element to add
     */
    @Ensure({"{old has({arg 1})} || (count() == {old count()} + 1)",
            "has({arg 1})",
            "generation() == {old generation()} + 1"})
    void put(G element);

    /**
     * Put the given element. The element must not already
     * be in the Set.
     *
     * @param element the element to add
     */
    @Require("!has({arg 1})")
    @Ensure({"count() == {old count()} + 1",
            "has({arg 1})",
            "generation() == {old generation()} + 1"})
    void add(G element);

    /**
     * Remove an element from the Set.
     */
    @Require("has({arg 1})")
    @Ensure({"count() == {old count()} - 1",
            "!has({arg 1})",
            "generation() == {old generation()} + 1"})
    void del(G element);

}
