package net.cadrian.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import net.cadrian.incentive.error.RequireError;

/**
 *
 * @author cadrian
 */
public class TestHashedMap {

    /**
     * Nominal case
     */
    @Test
    public void testNominal() {
        final HashedMap<Integer, String> map = new HashedMap<Integer, String>();
        assertEquals(0, map.count());

        map.add(1, "foo");
        assertEquals(1, map.count());
        assertEquals(Integer.valueOf(1), map.key(0));
        assertEquals("foo", map.value(0));
        assertEquals("foo", map.at(1));
        assertEquals("foo", map.ref(1));
        assertNull(map.ref(2));

        map.add(2, "bar");
        assertEquals(2, map.count());
        assertEquals(Integer.valueOf(1), map.key(0));
        assertEquals("foo", map.value(0));
        assertEquals("foo", map.at(1));
        assertEquals("foo", map.ref(1));
        assertEquals(Integer.valueOf(2), map.key(1));
        assertEquals("bar", map.value(1));
        assertEquals("bar", map.at(2));
        assertEquals("bar", map.ref(2));
        assertNull(map.ref(3));
    }

    /**
     * Try to fetch a non-existing item
     */
    @Test(expected = RequireError.class)
    public void testCannotAt() {
        final HashedMap<Integer, String> map = new HashedMap<Integer, String>();
        map.add(1, "foo");
        map.at(2);
    }

    /**
     * Try to add an already existing item
     */
    @Test(expected = RequireError.class)
    public void testCannotAdd() {
        final HashedMap<Integer, String> map = new HashedMap<Integer, String>();
        map.add(1, "foo");
        map.add(1, "bar");
    }

    /**
     * Nominal case with array rehash
     */
    @Test
    public void testRehash() {
        final HashedMap<Integer, String> map = new HashedMap<Integer, String>();
        map.add(1, "one");
        map.add(2, "two");
        map.add(3, "three");
        map.add(4, "four");
        map.add(5, "five");
        map.add(6, "six");

        assertEquals(6, map.count());
        assertEquals("one", map.at(1));
        assertEquals("two", map.at(2));
        assertEquals("three", map.at(3));
        assertEquals("four", map.at(4));
        assertEquals("five", map.at(5));
        assertEquals("six", map.at(6));
    }

    /**
     * Nominal case with collision
     */
    @Test
    public void testCollision() {
        final HashedMap<Integer, String> map = new HashedMap<Integer, String>();
        map.add(1, "one");
        map.add(2, "two");
        map.add(5, "five");

        assertEquals(3, map.count());
        assertEquals("one", map.at(1));
        assertEquals("two", map.at(2));
        assertEquals("five", map.at(5));
    }

    /**
     * Nominal case with collision and key removal
     */
    @Test
    public void testRemoval() {
        final HashedMap<Integer, String> map = new HashedMap<Integer, String>();
        map.add(1, "one");
        map.add(2, "two");
        map.add(5, "five");

        map.del(1);

        assertEquals(2, map.count());
        assertEquals("two", map.at(2));
        assertEquals("five", map.at(5));
    }

}
