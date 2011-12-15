package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

/**
 * A stack of elements: the abstract interface
 *
 * @author cadrian
 *
 * @param <G>
 *            the type of elements
 */
@DBC
public interface Stack<G> extends Collection<G> {

    /**
     * @return the top element of the stack
     */
    @Ensure("{result} == item(count()-1)")
    public G top();

    /**
     * @param g
     *            the element to push onto the stack
     */
    @Ensure({ "count() == {old count()} + 1", "top() == {arg 1}" })
    public <E extends G> void push(E element);

    /**
     * remove the top item from the stack
     */
    @Require("!isEmpty()")
    @Ensure("count() == {old count()} - 1")
    public void pop();
}
