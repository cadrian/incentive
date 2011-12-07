package net.cadrian.incentive.assist;

import java.util.HashMap;
import java.util.Map;

public enum Option {

	require_check {
		@Override
		void whenSet(final String value) {
			ensure_check.set = false;
			invariant_check.set = false;
		}
	},
	ensure_check {
		@Override
		void whenSet(final String value) {
			require_check.set(value);
			invariant_check.set = false;
		}
	},
	invariant_check {
		@Override
		void whenSet(final String value) {
			require_check.set(value);
			invariant_check.set(value);
		}
	},
	cache {
		@Override
		void whenSet(final String value) {
		}
	};

	private static final Map<String, Option> options = new HashMap<String, Option>();
	static {
		for (final Option o : values()) {
			options.put(o.name(), o);
		}
	}

	static boolean has(final String name) {
		return options.containsKey(name);
	}

	static String get(final String name) {
		String result = null;
		final Option option = options.get(name);
		if (option != null) {
			result = option.getValue();
		}
		return result;
	}

	static boolean isSet(final String name) {
		return options.get(name).isSet();
	}

	static void set(final String name, final String value) {
		options.get(name).setValue(value);
	}

	private String value = null;
	private boolean set = false;

	public boolean isSet() {
		return set;
	}

	public String getValue() {
		if (!set) {
			return null;
		}
		return value;
	}

	void setValue(final String value) {
		set(value);
		whenSet(value);
	}

	private void set(final String value) {
		this.value = value;
		this.set = true;
	}

	abstract void whenSet(String value);

}
