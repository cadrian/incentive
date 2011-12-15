package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;
import net.cadrian.incentive.Require;

/**
 * A collection of elements
 * 
 * @author cadrian
 * 
 * @param <G>
 *            the type of elements
 */
@DBC
@Invariant({ "count() >= 0" })
public interface Collection<G> {

	/**
	 * @return the number of elements in the collection
	 */
	public int count();

	/**
	 * @param i
	 *            the index of the element to return
	 * @return the i-th element in the collection
	 */
	@Require("{arg 1} >= 0 && {arg 1} < count()")
	public G item(int i);

	/**
	 * @return <code>true</code> if the stack is empty, <code>false</code>
	 *         otherwise
	 */
	@Ensure("{result} == (count() == 0)")
	public boolean isEmpty();

}
