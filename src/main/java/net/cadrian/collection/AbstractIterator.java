package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;

@DBC
abstract class AbstractIterator<G, I extends Iterable<G>> implements Iterator<G> {

    protected final I iterable;
    protected final int generation;

    @Ensure({"iterable == {arg 1}",
            "generation == {arg 1}.generation()"})
    AbstractIterator(final I a_iterable) {
        this.iterable = a_iterable;
        this.generation = a_iterable.generation();
    }

    @Override
    @Ensure("{result} == (generation == iterable.generation())")
    public boolean isValid() {
        return iterable.generation() == generation;
    }

    @Override
    @Ensure("{result} == generation")
    public int generation() {
        return generation;
    }

    @Override
    public Iterator<G> iterator() {
        return this;
    }

    @Override
    public <T> Map<G, T> map(final Agent<G, T> mapper) {
        final WritableMap<G, T> result = new HashedMap<G, T>();
        while (!isEmpty()) {
            final T value = mapper.run(item(), null);
            result.add(item(), value);
            next();
        }
        return result;
    }

    @Override
    public <T> T reduce(final Agent<G, T> reducer, final T seed) {
        T result = seed;
        while (!isEmpty()) {
            result = reducer.run(item(), result);
            next();
        }
        return result;
    }

    @Override
    public void doAll(final Agent<G, Void> agent) {
        while (!isEmpty()) {
            agent.run(item(), null);
            next();
        }
    }

}
