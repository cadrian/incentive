package net.cadrian.collection;

import static org.junit.Assert.assertEquals;
import net.cadrian.incentive.error.RequireError;

import org.junit.Test;

public class TestArrayStack {

	@Test
	public void testNominal() {
		final Stack<String> stack = new ArrayStack<String>();
		assertEquals(0, stack.count());
		stack.push("x");
		assertEquals(1, stack.count());
		assertEquals("x", stack.top());
		stack.push("y");
		assertEquals(2, stack.count());
		assertEquals("y", stack.top());
		stack.pop();
		assertEquals(1, stack.count());
		assertEquals("x", stack.top());
		stack.pop();
		assertEquals(0, stack.count());
	}

	@Test(expected = RequireError.class)
	public void testCannotPop() {
		final Stack<String> stack = new ArrayStack<String>();
		assertEquals(0, stack.count());
		stack.pop();
	}

}
