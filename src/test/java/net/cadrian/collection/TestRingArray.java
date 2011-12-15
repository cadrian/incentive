package net.cadrian.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 
 * @author cadrian
 */
public class TestRingArray {

	/**
	 * Nominal case
	 */
	@Test
	public void testNominal() {
		final RingArray<String> array = new RingArray<String>();
		assertEquals(0, array.count());
		array.addLast("foo");
		assertEquals(1, array.count());
		assertEquals("foo", array.item(0));
		array.addLast("bar");
		assertEquals(2, array.count());
		assertEquals("foo", array.item(0));
		assertEquals("bar", array.item(1));
		array.addFirst("head");
		assertEquals(3, array.count());
		assertEquals("head", array.item(0));
		assertEquals("foo", array.item(1));
		assertEquals("bar", array.item(2));
		array.removeLast();
		assertEquals(2, array.count());
		assertEquals("head", array.item(0));
		assertEquals("foo", array.item(1));
		array.removeFirst();
		assertEquals(1, array.count());
		assertEquals("foo", array.item(0));
	}

}
