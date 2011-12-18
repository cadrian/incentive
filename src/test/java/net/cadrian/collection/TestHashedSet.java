package net.cadrian.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import net.cadrian.incentive.error.RequireError;

/**
 *
 * @author cadrian
 */
public class TestHashedSet {

    /**
     * Nominal case
     */
    @Test
    public void testNominal() {
        final HashedSet<String> set = new HashedSet<String>();
        assertEquals(0, set.count());

        set.add("foo");
        assertEquals(1, set.count());
        assertEquals("foo", set.item(0));
        assertSame("foo", set.at(new String("foo")));
        assertSame("foo", set.ref(new String("foo")));

        set.add("bar");
        assertEquals(2, set.count());
        assertEquals("foo", set.item(0));
        assertSame("foo", set.at(new String("foo")));
        assertSame("foo", set.ref(new String("foo")));
        assertEquals("bar", set.item(1));
        assertEquals("bar", set.at(new String("bar")));
        assertEquals("bar", set.ref(new String("bar")));
    }

    /**
     * Try to fetch a non-existing item
     */
    @Test(expected = RequireError.class)
    public void testCannotAt() {
        final HashedSet<String> set = new HashedSet<String>();
        set.add("foo");
        set.at("bar");
    }

    /**
     * Try to add an already existing item
     */
    @Test(expected = RequireError.class)
    public void testCannotAdd() {
        final HashedSet<String> set = new HashedSet<String>();
        set.add("foo");
        set.add(new String("foo"));
    }

    /**
     * Nominal case with array rehash
     */
    @Test
    public void testRehash() {
        final HashedSet<String> set = new HashedSet<String>();
        set.add("one");
        set.add("two");
        set.add("three");
        set.add("four");
        set.add("five");
        set.add("six");

        assertEquals(6, set.count());
        assertEquals("one",   set.at(new String("one")));
        assertEquals("two",   set.at(new String("two")));
        assertEquals("three", set.at(new String("three")));
        assertEquals("four",  set.at(new String("four")));
        assertEquals("five",  set.at(new String("five")));
        assertEquals("six",   set.at(new String("six")));
    }

    /**
     * Nominal case with collision
     */
    @Test
    public void testCollision() {
        final HashedSet<Integer> set = new HashedSet<Integer>();
        set.add(1);
        set.add(2);
        set.add(5);

        assertEquals(3, set.count());
        assertEquals(Integer.valueOf(1), set.at(1));
        assertEquals(Integer.valueOf(2), set.at(2));
        assertEquals(Integer.valueOf(5), set.at(5));
    }

    /**
     * Nominal case with collision and key removal
     */
    @Test
    public void testRemoval() {
        final HashedSet<Integer> set = new HashedSet<Integer>();
        set.add(1);
        set.add(2);
        set.add(5);

        set.del(1);

        assertEquals(2, set.count());
        assertEquals(Integer.valueOf(2), set.at(2));
        assertEquals(Integer.valueOf(5), set.at(5));
    }

    /**
     * iterator
     */
    @Test
    public void testIterator() {
        final HashedSet<String> set = new HashedSet<String>();
        set.add("one");
        set.add("two");
        set.add("five");

        set.del("one");

        final Iterator<String> iter = set.iterator();
        assertEquals(2, iter.count());
        assertEquals("five", iter.item());
        iter.next();
        assertEquals(1, iter.count());
        assertEquals("two", iter.item());
        iter.next();
        assertEquals(0, iter.count());
    }

    /**
     * iterator on changed underlying set
     */
    @Test(expected = RequireError.class)
    public void testIteratorNextGeneration() {
        final HashedSet<String> set = new HashedSet<String>();
        set.add("one");
        set.add("two");
        set.add("five");

        final Iterator<String> iter = set.iterator();
        assertEquals(3, iter.count());
        assertEquals("five", iter.item());

        set.del("one");

        iter.next();
    }

    /**
     * iterator.next on empty
     */
    @Test(expected = RequireError.class)
    public void testIteratorNextEmpty() {
        final HashedSet<String> set = new HashedSet<String>();
        set.add("one");
        set.add("two");
        set.add("five");

        final Iterator<String> iter = set.iterator();
        iter.next();
        iter.next();
        iter.next();
        assertEquals(0, iter.count());

        iter.next();
    }

}
