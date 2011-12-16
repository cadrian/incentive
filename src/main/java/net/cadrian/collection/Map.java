package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

@DBC
public interface Map<K, V> extends Collection<MapEntry<K, V>> {

    /**
     * @param the key to look at
     *
     * @return <code>true</code> if the key is in the Map,
     * <code>false</code> otherwise
     */
    @Ensure("(count() > 0) || !{result}")
    boolean has(K key);

    /**
     * @param the key to look at
     *
     * @return the value at the given key
     */
    @Require("has({arg 1})")
    V at(K key);

    /**
     * @param index the index of the key to return
     *
     * @return the index-th key
     */
    @Require("{arg 1} >= 0 && {arg 1} < count()")
    @Ensure("{result} == item({arg 1}).key()")
    K key(int index);

    /**
     * @param index the index of the value to return
     *
     * @return the index-th value
     */
    @Require("{arg 1} >= 0 && {arg 1} < count()")
    @Ensure("{result} == item({arg 1}).value()")
    V value(int index);

}
