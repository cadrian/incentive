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
     * @param i the index of the element to return
     * @return the i-th element in the collection
     */
    @Require("{arg 1} >= 0 && {arg 1} < count()")
    public G item(int index);

    /**
     * @return <code>true</code> if the stack is empty, <code>false</code>
     *         otherwise
     */
    @Ensure("{result} == (count() == 0)")
    public boolean isEmpty();

    /**
     * @param array the array to fill, if big enough; otherwise it
     *            gives the runtime type of the array to return.
     *
     * @return an array containing the elements. If the given array is
     * big enough, the first <code>count()</code> elements are those
     * of the collection. Otherwise, a new array is returned.
     */
    @Ensure({"{result} != null",
            "{result}.length == count()",
            "{arg 1} == null || {arg 1}.length <= count() || {result} == {arg 1}"})
    public G[] toArray(final G[] array);

}
