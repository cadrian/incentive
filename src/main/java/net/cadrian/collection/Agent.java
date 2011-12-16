package net.cadrian.collection;

import net.cadrian.incentive.DBC;

@DBC
public interface Agent<G, R> {

    R run(final G element, final R data);

}
