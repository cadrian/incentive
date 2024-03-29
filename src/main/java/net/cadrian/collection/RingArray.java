package net.cadrian.collection;

import java.lang.reflect.Array;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

/**
 * An array with O(1) insertion and removal at both ends of the array (except
 * when resize is needed) and O(1) access to elements
 *
 * @author cadrian
 *
 * @param <G>
 */
@DBC
public class RingArray<G> extends AbstractCollection<G> {

    private G[] items;
    private int lower;
    private int count;

    /**
     * Create a new array
     */
    @Ensure("count() == 0")
    public RingArray() {
        this(4);
    }

    @Ensure("count() == 0")
    @SuppressWarnings("unchecked")
    RingArray(final int capacity) {
        items = (G[]) new Object[capacity];
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public boolean has(final G element) {
        boolean result = false;
        for (int i = 0; !result && i < count(); i++) {
            final G elt = item(i);
            result = elt == element || (elt != null && elt.equals(element));
        }
        return result;
    }

    @Override
    public G item(final int index) {
        final int actualIndex = (index + lower) % items.length;
        return items[actualIndex];
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
    @Ensure({"count() == {old count()} + 1",
            "item(count()-1) == {arg 1}",
            "generation() == {old generation()} + 1"})
    public void addLast(final G element) {
        if (count == items.length) {
            makeRoom();
        }
        final int index = (lower + count) % items.length;
        items[index] = element;
        count++;
        generation++;
    }

    /**
     * Remove the last element
     */
    @Require("count() > 0")
    @Ensure({"count() == {old count()} - 1",
            "generation() == {old generation()} + 1"})
    public void removeLast() {
        count--;
        generation++;
    }

    /**
     * Add an element at the head of the array
     *
     * @param element
     *            the element to insert
     */
    @Ensure({"count() == {old count()} + 1",
            "item(0) == {arg 1}",
            "generation() == {old generation()} + 1"})
    public void addFirst(final G element) {
        if (count == items.length) {
            makeRoom();
        }
        lower = (lower - 1 + items.length) % items.length;
        items[lower] = element;
        count++;
        generation++;
    }

    /**
     * Remove the first element
     */
    @Require("count() > 0")
    @Ensure({"count() == {old count()} - 1",
            "count() == 0 || item(0) == {old count() < 2 ? null : item(1)}",
            "generation() == {old generation()} + 1"})
    public void removeFirst() {
        count--;
        lower = (lower + 1) % items.length;
        generation++;
    }

    @Require("{arg 1}.length >= count()")
    private void copyTo(final G[] array) {
        final int pivot = count - lower;
        System.arraycopy(items, lower, array, 0, pivot);
        System.arraycopy(items, 0, array, pivot, lower);
    }

    @Ensure("count() == {old count()}")
    private void makeRoom() {
        @SuppressWarnings("unchecked")
        final G[] newArray = (G[]) new Object[count * 2];
        copyTo(newArray);
        items = newArray;
        lower = 0;
    }

    public G[] toArray(final G[] array) {
        final G[] result;
        if (array == null) {
            @SuppressWarnings("unchecked")
            final G[] newArray = (G[])new Object[count()];
            result = newArray;
        }
        else if (array.length < count()) {
            @SuppressWarnings("unchecked")
            final G[] newArray = (G[])Array.newInstance(array.getClass().getComponentType(), count());
            result = newArray;
        }
        else {
            result = array;
        }
        copyTo(result);
        return result;
    }

}
