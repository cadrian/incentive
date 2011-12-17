package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

@DBC
public interface WritableMap<K, V> extends Map<K, V> {

    /**
     * Put the given value at the given key. The key may already be
     * present, in that case its value is updated.
     *
     * @param key the key to update
     * @param value the new value of the key
     */
    @Ensure({"{old has({arg 1})} || (count() == {old count()} + 1)",
            "has({arg 1})",
            "at({arg 1}) == {arg 2}"})
    void put(K key, V value);

    /**
     * Put the given valye at the given key. The key must not already
     * be in the Map.
     *
     * @param key the key to add
     * @param value the value of the key
     */
    @Require("!has({arg 1})")
    @Ensure({"count() == {old count()} + 1",
            "has({arg 1})",
            "at({arg 1}) == {arg 2}"})
    void add(K key, V value);

}
