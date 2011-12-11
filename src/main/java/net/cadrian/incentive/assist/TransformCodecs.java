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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TransformCodecs {

	private TransformCodecs() {
	}

	static final Pattern RESULT_REGEXP = Pattern.compile("\\{result\\}");

	static final Pattern OLD_REGEXP = Pattern.compile("\\{old\\s+([^}]+)\\}");

	static final Pattern ARG_REGEXP = Pattern
			.compile("\\{arg\\s+([1-9][0-9]*)\\}");

	// The {result} -- in postconditions only
	static TransformCodec POSTCONDITION_RESULT_CODEC = new TransformCodec() {
		@Override
		public String decode(final String assertion) {
			return RESULT_REGEXP.matcher(assertion).replaceAll("$2");
		}
	};

	// The {old} values -- in preconditions only
	static TransformCodec PRECONDITION_OLD_VALUES_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = OLD_REGEXP.matcher(assertion);
			int i = 0;
			while (matcher.find()) {
				result.append(String.format("%s.set(%d, %s);",
						BehaviorInstrumentor.OLD_LOCAL_VAR, i++,
						matcher.group(1)));
			}
			return result.toString();
		}
	};

	// The {old} values -- in postconditions only
	static TransformCodec POSTCONDITION_OLD_VALUES_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = OLD_REGEXP.matcher(assertion);
			int i = 0;
			while (matcher.find()) {
				matcher.appendReplacement(result, String.format("%s.get(%d)",
						BehaviorInstrumentor.OLD_LOCAL_VAR, i++));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	};

	static TransformCodec PRECONDITION_ARGUMENTS_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = ARG_REGEXP.matcher(assertion);
			while (matcher.find()) {
				final int index = Integer.parseInt(matcher.group(1));
				matcher.appendReplacement(result, String.format("$%d", index));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	};

	static TransformCodec POSTCONDITION_ARGUMENTS_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = ARG_REGEXP.matcher(assertion);
			while (matcher.find()) {
				final int index = Integer.parseInt(matcher.group(1));
				// the first two indexes are the "old" array and the result
				matcher.appendReplacement(result,
						String.format("$%d", index + 2));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	};

}
