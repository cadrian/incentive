package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

@DBC
public interface Set<G> extends Collection<G> {

    /**
     * @param the element to look at
     *
     * @return the element in the set
     */
    @Require({"{arg 1} != null",
            "has({arg 1})"})
    @Ensure("{result}.equals({arg 1})")
    G at(G element);

    /**
     * @param the element to look at
     *
     * @return the element in the set
     */
    @Require("{arg 1} != null")
    @Ensure("!has({arg 1}) || {result}.equals({arg 1})")
    G ref(G element);

}
