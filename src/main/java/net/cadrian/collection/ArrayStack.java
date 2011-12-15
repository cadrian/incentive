package net.cadrian.collection;

import java.util.ArrayList;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;

/**
 * A {@link Stack} implemented using a simple array
 * 
 * @author cadrian
 * 
 * @param <G>
 */
@DBC
public class ArrayStack<G> implements Stack<G> {

	private final ArrayList<G> items;

	@SuppressWarnings("javadoc")
	@Ensure("count() == 0")
	public ArrayStack() {
		items = new ArrayList<G>();
	}

	@Override
	public int count() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.isEmpty();
	}

	@Override
	public G top() {
		return items.get(items.size() - 1);
	}

	@Override
	public <E extends G> void push(final E g) {
		items.add(g);
	}

	@Override
	public void pop() {
		items.remove(items.size() - 1);
	}

	@Override
	public G item(final int i) {
		return items.get(i);
	}

}
