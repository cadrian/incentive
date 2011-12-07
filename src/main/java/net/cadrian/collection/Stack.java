package net.cadrian.collection;

import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;
import net.cadrian.incentive.Require;

@Invariant({"count() >= 0"})
public interface Stack<G> {
	public int count();
	
	@Ensure({"return == (count() == 0)"})
	public boolean isEmpty();
	
	public G top();
	
	@Ensure({"count() == old count() + 1", "top() == g"})
	public <E extends G> void push(E g);
	
	@Require({"!isEmpty()"})
	@Ensure({"count() == old count() - 1"})
	public void pop();
}
