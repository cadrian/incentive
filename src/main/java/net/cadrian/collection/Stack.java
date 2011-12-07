package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;
import net.cadrian.incentive.Require;

@DBC
@Invariant({ "count() >= 0" })
public interface Stack<G> {
	public int count();

	@Ensure({ "{result} == (count() == 0)" })
	public boolean isEmpty();

	public G top();

	@Ensure({ "count() == {old count()} + 1", "top() == {arg 1}" })
	public <E extends G> void push(E g);

	@Require({ "!isEmpty()" })
	@Ensure({ "count() == {old count()} - 1" })
	public void pop();
}
