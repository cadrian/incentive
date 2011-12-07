package net.cadrian.incentive.assist;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AssertionCodecs {

	private AssertionCodecs() {
	}

	private static final Pattern RESULT_REGEXP = Pattern
			.compile("\\{result\\}");

	private static final Pattern OLD_REGEXP = Pattern
			.compile("\\{old\\s+([^}]+)\\}");

	private static final Pattern ARG_REGEXP = Pattern
			.compile("\\{arg\\s+([1-9][0-9]*)\\}");

	// The {result} -- in postconditions only
	static AssertionCodec RESULT_CODEC = new AssertionCodec() {
		@Override
		public String decode(final String assertion) {
			return RESULT_REGEXP.matcher(assertion).replaceAll("$2");
		};
	};

	// The {old} values -- in preconditions only
	static AssertionCodec PRECONDITION_OLD_VALUES_CODEC = new AssertionCodec() {
		@Override
		public String decode(final String assertion) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = OLD_REGEXP.matcher(assertion);
			int i = 0;
			while (matcher.find()) {
				matcher.appendReplacement(result, String.format(
						"%s.set(%d, %s)", BehaviorInstrumentor.OLD_LOCAL_VAR,
						i++, matcher.group(1)));
			}
			matcher.appendTail(result);
			return result.toString();
		};
	};

	// The {old} values -- in postconditions only
	static AssertionCodec POSTCONDITION_OLD_VALUES_CODEC = new AssertionCodec() {
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
		};
	};

	static AssertionCodec PRECONDITION_ARGUMENTS_CODEC = new AssertionCodec() {
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
		};
	};

	static AssertionCodec POSTCONDITION_ARGUMENTS_CODEC = new AssertionCodec() {
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
		};
	};

}
