package net.cadrian.incentive.check;

import static org.junit.Assert.*;

import net.cadrian.incentive.Require;
import net.cadrian.incentive.check.Checker;
import net.cadrian.incentive.check.PreconditionChecker;
import net.cadrian.incentive.error.RequireError;

import org.junit.Test;

public class PreconditionCheckerTestCase {

	private static interface IChecked {
		@Require({ "i > 0}" })
		public int foo(int i);
	}

	private static class Checked implements IChecked {
		public int foo(int i) {
			return i;
		}
	}

	@Test
	public void testOK() {
		new PreconditionChecker().run(new Runnable() {
			public void run() {
				Checked checked = new Checked();
				int a = checked.foo(42);
				assertEquals(42, a);
			}
		});
	}

	@Test
	public void testKO() {
		Checker checker = new PreconditionChecker();
		try {
			checker.run(new Runnable() {
				public void run() {
					Checked checked = new Checked();
					checked.foo(-4);
				}
			});
		} catch (RequireError rx) {
			// OK
		}
		fail("expected require exception");
	}
}
