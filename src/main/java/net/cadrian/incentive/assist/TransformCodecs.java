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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.compiler.CompileError;
import javassist.compiler.Lex;
import javassist.compiler.Parser;
import javassist.compiler.SymbolTable;
import javassist.compiler.ast.ASTree;

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
		public String decode(final String assertion, final CtClass targetClass,
				final ClassPool pool) {
			return RESULT_REGEXP.matcher(assertion).replaceAll("\\$2");
		}
	};

	// The {old} class -- in preconditions only
	static TransformCodec PRECONDITION_OLD_CLASS_CODEC(
			final CtClass preconditionClass) {
		final int[] index = { 0 };
		return new TransformCodec() {
			@SuppressWarnings("boxing")
			@Override
			public String decode(final String assertion,
					final CtClass targetClass, final ClassPool pool)
					throws CannotCompileException, CompileError {
				final StringBuffer result = new StringBuffer();
				final Matcher matcher = OLD_REGEXP.matcher(assertion);
				while (matcher.find()) {
					final String src = matcher.group(1);
					final CtClass type = expressionType(src, targetClass, pool);
					final CtField field = new CtField(type, String.format(
							"old%d", index[0]++), preconditionClass);
					field.setModifiers(Modifier.PUBLIC);
					preconditionClass.addField(field);
				}

				return result.toString();
			}
		};
	}

	// The {old} values -- in preconditions only
	static TransformCodec PRECONDITION_OLD_VALUES_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion, final CtClass targetClass,
				final ClassPool pool) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = OLD_REGEXP.matcher(assertion);
			int i = 0;
			while (matcher.find()) {
				final String src = matcher.group(1);
				result.append(String.format("result.old%d = (%s);\n", i++, src));
			}

			return result.toString();
		}
	};

	// The {old} values -- in postconditions only
	static TransformCodec POSTCONDITION_OLD_VALUES_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion, final CtClass targetClass,
				final ClassPool pool) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = OLD_REGEXP.matcher(assertion);
			int i = 0;
			while (matcher.find()) {
				matcher.appendReplacement(result,
						String.format("((\\$1).old%d)", i++));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	};

	static TransformCodec PRECONDITION_ARGUMENTS_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion, final CtClass targetClass,
				final ClassPool pool) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = ARG_REGEXP.matcher(assertion);
			while (matcher.find()) {
				final int index = Integer.parseInt(matcher.group(1));
				matcher.appendReplacement(result, String.format("\\$%d", index));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	};

	static TransformCodec POSTCONDITION_ARGUMENTS_CODEC = new TransformCodec() {
		@SuppressWarnings("boxing")
		@Override
		public String decode(final String assertion, final CtClass targetClass,
				final ClassPool pool) {
			final StringBuffer result = new StringBuffer();
			final Matcher matcher = ARG_REGEXP.matcher(assertion);
			while (matcher.find()) {
				final int index = Integer.parseInt(matcher.group(1));
				// the first two indexes are the "old" array and the result
				matcher.appendReplacement(result,
						String.format("\\$%d", index + 2));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	};

	static CtClass expressionType(final String src, final CtClass targetClass,
			final ClassPool pool) throws CompileError {
		final Parser p = new Parser(new Lex(src));
		final SymbolTable stb = new SymbolTable();
		final ASTree s = p.parseExpression(stb);
		return new StmtTypeVisitor(targetClass, pool).getType(s);
	}

}
