package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;
import net.cadrian.incentive.Require;

@DBC
@Invariant({ "count() >= 0" })
public interface Iterable<G> {

    /**
     * @return the number of elements in the collection
     */
    public int count();

    /**
     * @return <code>true</code> if the stack is empty, <code>false</code>
     *         otherwise
     */
    @Ensure("{result} == (count() == 0)")
    public boolean isEmpty();

    /**
     * @return a new iterator on the objects in the collection
     */
    @Ensure({"{result} != null",
            "(count() == 0) == ({result}.isEmpty())"})
    public Iterator<G> iterator();

    /**
     * map operation
     *
     * @parem mapper the mapper agent
     *
     * @return a map resulting from applying `mapper` to the elements
     * of the iterable
     */
    @Require("{arg 1} != null")
    @Ensure({"count() == {old count()}",
            "{result}.count() == count()"})
    <T> Map<G, T> map(final Agent<G, T> mapper);

    /**
     * reduce operation
     *
     * @parem reducer the reduce agent
     * @param seed to bootstrap reduction
     *
     * @return a map resulting from applying `mapper` to the elements
     * of the iterable
     */
    @Require("{arg 1} != null")
    @Ensure({"count() == {old count()}",
            "count() > 0 || {result} == {arg 2}"})
    <T> T reduce(final Agent<G, T> reducer, final T seed);

    /**
     * do something using each element
     *
     * @param agent the agent to execute on each element
     */
    @Require("{arg 1} != null")
    @Ensure("count() == {old count()}")
    void doAll(final Agent<G, Void> agent);

}
