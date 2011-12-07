package net.cadrian.incentive.assist;

import java.util.HashMap;
import java.util.Map;

public enum Option {

	require_check, ensure_check, invariant_check, cache;

	private static final Map<String, Option> options = new HashMap<String, Option>();
	static {
		for (final Option o : values()) {
			options.put(o.name(), o);
		}
	}

	static boolean has(final String name) {
		return options.containsKey(name);
	}

	static Option get(final String name) {
		return options.get(name);
	}

	private String value = null;

	public String getValue() {
		return value;
	}

	void setValue(final String value) {
		this.value = value;
	}

}
