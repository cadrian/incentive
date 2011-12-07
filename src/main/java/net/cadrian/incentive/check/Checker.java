package net.cadrian.incentive.check;

import java.lang.reflect.Proxy;

import net.cadrian.incentive.Invariant;

@Invariant("getParent() != null")
abstract class Checker extends ClassLoader {

	public Checker() {
		super(ClassLoader.getSystemClassLoader());
	}

	public Checker(ClassLoader cl) {
		super(cl);
	}

	public void run(Runnable run) {
		run.run();
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		final Class<?> result;
		Class<?> original = super.findClass(name);
		Class<?>[] interfaces = original.getInterfaces();
		if (interfaces != null && interfaces.length > 0) {
			Class<?> proxy = Proxy.getProxyClass(getParent(), interfaces);
			System.out.println("PROXY");
			result = proxy;
		} else {
			System.out.println("ORIGINAL");
			result = original;
		}
		return result;
	}

}