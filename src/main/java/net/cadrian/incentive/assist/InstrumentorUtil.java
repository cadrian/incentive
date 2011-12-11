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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.compiler.CompileError;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import javassist.expr.Instanceof;
import javassist.expr.MethodCall;
import javassist.expr.NewArray;
import javassist.expr.NewExpr;
import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Pure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InstrumentorUtil {
	private static final Logger LOG = LoggerFactory
			.getLogger(InstrumentorUtil.class);

	public static boolean instrumentedWith(final CtBehavior method,
			final String methodName, final String signature)
			throws CannotCompileException {
		final boolean[] tmp = { false };
		method.instrument(new ExprEditor() {
			@Override
			public void edit(final MethodCall m) {
				if (m.getMethodName().equals(methodName)
						&& m.getSignature().equals(signature)) {
					tmp[0] = true;
				}
			}
		});
		return tmp[0];
	}

	public static boolean methodAnnotatedWithPure(final CtMethod method)
			throws ClassNotFoundException {
		for (final Object annotation : method.getAnnotations()) {
			if (annotation instanceof Pure) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPure(final CtMethod method)
			throws CannotCompileException, NotFoundException,
			ClassNotFoundException {
		if (methodAnnotatedWithPure(method)) {
			return true;
		}

		final List<CtClass> classHierarchy = getParents(method
				.getDeclaringClass());
		classHierarchy.add(0, method.getDeclaringClass());
		for (final CtClass c : classHierarchy) {
			final List<CtClass> interfaces = new ArrayList<CtClass>();
			Collections.addAll(interfaces, c.getInterfaces());
			for (int i = 0; i < interfaces.size(); i++) {
				final CtClass interfaze = interfaces.get(i);
				CtMethod m = null;
				m = interfaze
						.getMethod(method.getName(), method.getSignature());
				if (m != null && methodAnnotatedWithPure(m)) {
					return true;
				}
				Collections.addAll(interfaces, interfaze.getInterfaces());
			}
		}

		final boolean[] tmp = { true };
		method.instrument(new ExprEditor() {
			@Override
			public void edit(final Cast c) {
				// Should be ok
			}

			@Override
			public void edit(final ConstructorCall c) {
				tmp[0] = false;
			}

			@Override
			public void edit(final FieldAccess f) {
				if (!f.isReader()) {
					tmp[0] = false;
				}
			}

			@Override
			public void edit(final Handler h) {
				tmp[0] = false;
			}

			@Override
			public void edit(final Instanceof i) {
				// Should be ok
			}

			@Override
			public void edit(final MethodCall m) {
				tmp[0] = false;
			}

			@Override
			public void edit(final NewArray a) {
				tmp[0] = false;
			}

			@Override
			public void edit(final NewExpr e) {
				tmp[0] = false;
			}
		});
		return tmp[0];
	}

	private static void addParents(final CtClass targetClass,
			final Set<CtClass> parents) throws NotFoundException {
		final CtClass parent = targetClass.getSuperclass();
		if (parent != null && !parents.contains(parent)) {
			parents.add(parent);
			addParents(parent, parents);
		}
		for (final CtClass itf : targetClass.getInterfaces()) {
			if (!parents.contains(itf)) {
				parents.add(itf);
				addParents(itf, parents);
			}
		}
	}

	public static List<CtClass> getParents(final CtClass targetClass)
			throws NotFoundException {
		final Set<CtClass> parents = new LinkedHashSet<CtClass>();
		addParents(targetClass, parents);
		return new ArrayList<CtClass>(parents);
	}

	static boolean hasDBC(final CtClass a_targetClass) {
		try {
			final DBC dbc = (DBC) a_targetClass.getAnnotation(DBC.class);
			return dbc != null && !dbc.skip();
		} catch (final ClassNotFoundException cnfx) {
			LOG.warn("class not found", cnfx);
		}
		return false;
	}

	static String parseAssertions(final String[] assertions,
			final CtClass targetClass, final ClassPool pool,
			final String errorClassName, final String errorMessage,
			final TransformCodec... codecs) throws CannotCompileException,
			CompileError {
		final StringBuilder src = new StringBuilder();
		for (final String assertion : assertions) {
			src.append(String
					.format("{boolean b=false;try{b=(%s);}\ncatch(Throwable t){throw new %s(\"%s: \" + t.getMessage(), t);}\nif(!b) throw new %s(\"%s\");}\n",
							transform(assertion, targetClass, pool, codecs),
							errorClassName, errorMessage, errorClassName,
							errorMessage));
		}
		return src.toString();
	}

	static String transform(final String src, final CtClass targetClass,
			final ClassPool pool, final TransformCodec... codecs)
			throws CannotCompileException, CompileError {
		String result = src;
		if (codecs != null) {
			for (final TransformCodec codec : codecs) {
				result = codec.decode(result, targetClass, pool);
			}
		}
		return result;
	}

	static String voidify(final String descriptor) {
		final int index = descriptor.indexOf(')');
		if (index < 0) {
			return descriptor;
		}
		return String.format("%sV;", descriptor.substring(0, index + 1));
	}
}
