package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;

/**
 * A {@link Stack} implemented using a simple array
 *
 * @author cadrian
 *
 * @param <G>
 */
@DBC
@Invariant("items != null")
public class ArrayStack<G> extends AbstractCollection<G> implements Stack<G> {

    private final RingArray<G> items;

    @SuppressWarnings("javadoc")
    @Ensure("count() == 0")
    public ArrayStack() {
        items = new RingArray<G>();
    }

    @Override
    public int count() {
        return items.count();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public G top() {
        return items.item(0);
    }

    @Override
    public <E extends G> void push(final E element) {
        items.addFirst(element);
    }

    @Override
    public void pop() {
        items.removeFirst();
    }

    @Override
    public G item(final int index) {
        final int len = count() - 1;
        return items.item(len - index);
    }

    public G[] toArray(final G[] array) {
        final G[] result = items.toArray(array);
        final int len = count();
        final int mid = len / 2;
        if (mid > 0) {
            for (int i = 0; i < mid; i++) {
                final G tmp = array[i];
                array[i] = array[len - i];
                array[len - i] = tmp;
            }
        }
        return result;
    }

    public Iterator<G> iterator() {
        return new ArrayStackIterator<G>(this);
    }

}

@DBC
class ArrayStackIterator<G> extends AbstractIterator<G> {

    private final ArrayStack<G> array;
    private int index;

    @Ensure({"array == {arg 1}", "count() == {arg 1}.count()", "index == 0"})
    ArrayStackIterator(final ArrayStack<G> a_array) {
        this.array = a_array;
    }

    public int count() {
        return array.count() - index;
    }

    public boolean isEmpty() {
        return index == array.count();
    }

    public void next() {
        index++;
    }

    public G item() {
        return array.item(index);
    }
}
