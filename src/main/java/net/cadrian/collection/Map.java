package net.cadrian.collection;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;

@DBC
public interface Map<K, V> extends Iterable<MapEntry<K, V>> {

    /**
     * @param the key to look at
     *
     * @return <code>true</code> if the key is in the Map,
     * <code>false</code> otherwise
     */
    @Ensure("(count() > 0) || !{result}")
    boolean has(K key);

    /**
     * @param index the index of the element to return
     * @return the index-th element in the collection
     */
    @Require("{arg 1} >= 0 && {arg 1} < count()")
    public MapEntry<K, V> item(int index);

    /**
     * @param array the array to fill, if big enough; otherwise it
     *            gives the runtime type of the array to return.
     *
     * @return an array containing the elements. If the given array is
     * big enough, the first <code>count()</code> elements are those
     * of the collection. Otherwise, a new array is returned.
     */
    @Ensure({"{result} != null",
            "{result}.length == count()",
            "{arg 1} == null || {arg 1}.length <= count() || {result} == {arg 1}"})
    public MapEntry<K, V>[] toArray(final MapEntry<K, V>[] array);

    /**
     * @param the key to look at
     *
     * @return the value at the given key
     */
    @Require("has({arg 1})")
    V at(K key);

    /**
     * @param the key to look at
     *
     * @return the value at the given key
     */
    @Ensure("!has({arg 1}) || {result} == at({arg 1})")
    V ref(K key);

    /**
     * @param index the index of the key to return
     *
     * @return the index-th key
     */
    @Require("{arg 1} >= 0 && {arg 1} < count()")
    @Ensure("{result} == item({arg 1}).key")
    K key(int index);

    /**
     * @param index the index of the value to return
     *
     * @return the index-th value
     */
    @Require("{arg 1} >= 0 && {arg 1} < count()")
    @Ensure("{result} == item({arg 1}).value")
    V value(int index);

    /**
     * @return the set of keys
     */
    @Ensure({"{result}.count() == count()",
            "{forall(K k: {result}) has(k)}"})
    Set<K> keySet();

    /**
     * @return the collection of values
     */
    @Ensure({"{result}.count() == count()",
            "{forall(V v: {result}) {exists(K k: keySet()) at(k) == v}}"})
    Collection<V> values();

}
