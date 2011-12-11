/*
 * Incentive, A Design By Contract framework for Java.
 * Copyright (C) 2011 Cyril Adrian. All Rights Reserved.
 * 
 * Javaassist implementation based on C4J's 
 * Copyright (C) 2006 Jonas Bergstr�m. All Rights Reserved.
 *
 * The contents of this file may be used under the terms of the GNU Lesser 
 * General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */
package net.cadrian.incentive.assist;

import java.util.HashMap;
import java.util.Map;

enum Option {

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
			// nothing
		}
	},
	limit {
		@Override
		void whenSet(final String value) {
			// nothing
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
	boolean set = false;

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

	void set(final String value) {
		this.value = value;
		this.set = true;
	}

	abstract void whenSet(String a_value);

}
