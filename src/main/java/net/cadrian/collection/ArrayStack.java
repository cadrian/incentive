package net.cadrian.collection;

import java.util.ArrayList;

public class ArrayStack<G> implements Stack<G> {

	private final ArrayList<G> items;

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

}
