package net.cadrian.collection;

import net.cadrian.incentive.DBC;

@DBC
abstract class AbstractIterator<G> implements Iterator<G> {

    public Iterator<G> iterator() {
        return this;
    }

    public <T> Map<G, T> map(final Agent<G, T> mapper) {
        final WritableMap<G, T> result = new HashedMap<G, T>();
        while (!isOff()) {
            final T value = mapper.run(item(), null);
            result.add(item(), value);
            next();
        }
        return result;
    }

    public <T> T reduce(final Agent<G, T> reducer, final T seed) {
        T result = seed;
        while (!isOff()) {
            result = reducer.run(item(), result);
            next();
        }
        return result;
    }

    public void doAll(final Agent<G, Void> agent) {
        while (!isOff()) {
            agent.run(item(), null);
            next();
        }
    }

}
