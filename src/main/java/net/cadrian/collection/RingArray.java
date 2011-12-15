package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;

/**
 * An array with O(1) insertion and removal at both ends of the array (except
 * when resize is needed) and O(1) access to elements
 * 
 * @author cadrian
 * 
 * @param <G>
 */
@DBC
public class RingArray<G> implements Collection<G> {

	private G[] items;
	private int lower;
	private int count;

	/**
	 * Create a new array
	 */
	@Ensure("count() == 0")
	public RingArray() {
		@SuppressWarnings("unchecked")
		final G[] newArray = (G[]) new Object[16];
		items = newArray;
	}

	@Override
	public int count() {
		return count;
	}

	@Override
	public G item(final int i) {
		return items[(i + lower) % items.length];
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * Add an element at the tail of the array
	 * 
	 * @param element
	 *            the element to add
	 */
	@Ensure({ "count() == {old count()} + 1", "item(count()-1) == {arg 1}" })
	public void addLast(final G element) {
		if (count == items.length) {
			makeRoom();
		}
		count++;
		final int index = (lower + count) % items.length;
		items[index] = element;
	}

	/**
	 * Remove the last element
	 */
	@Ensure("count() == {old count()} - 1")
	public void removeLast() {
		count--;
	}

	/**
	 * Add an element at the head of the array
	 * 
	 * @param element
	 *            the element to insert
	 */
	@Ensure({ "count() == {old count()} + 1", "item(0) == {arg 1}" })
	public void addFirst(final G element) {
		if (count == items.length) {
			makeRoom();
		}
		count++;
		final int index = (lower + items.length - 1) % items.length;
		items[index] = element;
	}

	/**
	 * Remove the first element
	 */
	@Ensure({ "count() == {old count()} - 1",
			"count() == 0 || item(0) == {old item(1)}" })
	public void removeFirst() {
		count--;
		lower = (lower + 1) % items.length;
	}

	@Ensure("{count() == {old count()}")
	private void makeRoom() {
		@SuppressWarnings("unchecked")
		final G[] newArray = (G[]) new Object[count * 2];
		final int pivot = count - lower;
		System.arraycopy(items, lower, newArray, 0, pivot);
		System.arraycopy(items, 0, newArray, pivot, lower);
		items = newArray;
	}

}
