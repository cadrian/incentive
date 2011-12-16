package net.cadrian.collection;

import net.cadrian.incentive.DBC;

@DBC
public interface MapEntry<K, V> {

    K getKey();

    V getValue();

}
