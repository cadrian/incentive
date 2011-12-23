package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;

@DBC
class DefaultCollectionIterator<G> extends AbstractIterator<G, AbstractCollection<G>> {

    private int index;

    @Ensure({"iterable == {arg 1}",
            "count() == {arg 1}.count()",
            "index == 0",
            "generation == {arg 1}.generation()"})
    DefaultCollectionIterator(final AbstractCollection<G> a_collection) {
        super(a_collection);
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
    public void next() {
        index++;
    }

    @Override
    public G item() {
        return iterable.item(index);
    }

}
