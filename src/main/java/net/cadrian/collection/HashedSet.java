package net.cadrian.collection;

import java.lang.reflect.Array;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Invariant;

@DBC
@Invariant({"elements != null",
        "used != null",
        "elements.length == used.length"})
public class HashedSet<G> extends AbstractCollection<G> implements WritableSet<G> {

    private static final int MAP_FULL = -1;

    static class IterationCache {
        int externalIndex;
        int elementIndex;

        void update(final int ext, final int element) {
            externalIndex = ext;
            elementIndex = element;
        }
    }

    G[] elements;
    private boolean[] used;
    private int count;

    private final IterationCache indexCache;

    @Ensure("count() == 0")
    public HashedSet() {
        this(4);
    }

    @Ensure("count() == 0")
    HashedSet(final int capacity) {
        @SuppressWarnings("unchecked")
        final G[] newElements = (G[])new Object[capacity];
        final boolean[] newUsed = new boolean[capacity];

        elements = newElements;
        used = newUsed;

        indexCache = new IterationCache();
    }

    private int _indexOf(final G[] _elements, final boolean[] _used, final G element) {
        // we use Python's method of hashing elements
        int hash = element.hashCode();
        final int mask = _elements.length - 1;
        final int initial = hash & mask;
        int result = initial;
        while ((_used == null ? _elements[result] != null : _used[result]) && (_elements[result] == null || !_elements[result].equals(element))) {
            hash >>= 5;
            result = (5 * result + 1 + hash) & mask;
            if (result == initial) {
                result = MAP_FULL;
                break;
            }
        }

        return result;
    }

    private void grow() {
        final int newCapacity = elements.length * 2;
        @SuppressWarnings("unchecked")
        final G[] newElements = (G[])new Object[newCapacity];
        final boolean[] newUsed = new boolean[newCapacity];

        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null) {
                final int index = _indexOf(newElements, null, elements[i]);
                newElements[index] = elements[i];
                newUsed[index] = true;
            }
        }

        elements = newElements;
        used = newUsed;
    }

    private int indexOf(final G element, final boolean followUsed) {
        int result = _indexOf(elements, followUsed ? used : null, element);
        while (result == MAP_FULL) {
            grow();
            result = _indexOf(elements, followUsed ? used : null, element);
        }
        return result;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public boolean has(final G element) {
        final int index = _indexOf(elements, used, element);
        return index != MAP_FULL && elements[index] != null;
    }

    @Override
    public G at(final G element) {
        return elements[_indexOf(elements, used, element)];
    }

    @Override
    public G ref(final G element) {
        final int index = _indexOf(elements, used, element);
        if (index == MAP_FULL) {
            return null;
        }
        return elements[index];
    }

    private void findIndex(final IterationCache cache, final int index) {
        if (index != cache.externalIndex || elements[cache.elementIndex] == null) {
            int externalIndex = cache.externalIndex;
            int elementIndex = cache.elementIndex;

            if (externalIndex > index) {
                externalIndex = 0;
                elementIndex = 0;
            }
            else {
                elementIndex++;
            }

            while (externalIndex < index || elements[elementIndex] == null) {
                if (elements[elementIndex] == null) {
                    elementIndex++;
                }
                else {
                    externalIndex++;
                }
            }

            cache.update(externalIndex, elementIndex);
        }
    }

    G cachedItem(final int index, final IterationCache cache) {
        findIndex(cache, index);
        return elements[cache.elementIndex];
    }

    @Override
    public G item(final int index) {
        return cachedItem(index, indexCache);
    }

    @Override
    public void add(final G element) {
        final int index = indexOf(element, false);
        elements[index] = element;
        used[index] = true;
        count++;
        generation++;
    }

    @Override
    public void put(final G element) {
        int index = indexOf(element, true);
        if (elements[index] == null) {
            index = indexOf(element, false);
            count++;
            elements[index] = element;
        }
        used[index] = true;
        generation++;
    }

    @Override
    public void del(final G element) {
        final int index = indexOf(element, true);
        elements[index] = null;
        // but used[index] stays true to enable to follow collisions
        count--;
        generation++;
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
        for (int i = 0; i < count; i++) {
            result[i] = item(i);
        }
        return result;
    }

    @Override
    public Iterator<G> iterator() {
        return new HashedSetIterator<G>(this, new IterationCache());
    }

}

@DBC
class HashedSetIterator<G> extends AbstractIterator<G, HashedSet<G>> {

    private final HashedSet.IterationCache cache;
    private int index;

    HashedSetIterator(final HashedSet<G> set, final HashedSet.IterationCache cache) {
        super(set);
        this.cache = cache;
        index = 0;
    }

    @Override
    public int count() {
        return iterable.count() - index;
    }

    @Override
    public boolean isEmpty() {
        return index == iterable.count();
    }

    @Override
    public G item() {
        return iterable.cachedItem(index, cache);
    }

    @Override
    public void next() {
        index++;
    }

}
