package net.cadrian.incentive.assist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
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

public class InstrumentorUtil {
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

	public static boolean methodAnnotatedWithPure(final CtMethod method) {
		try {
			for (final Object annotation : method.getAnnotations()) {
				if (annotation instanceof Pure) {
					return true;
				}
			}
		} catch (final ClassNotFoundException e) {
		}
		return false;
	}

	public static boolean isPure(final CtMethod method)
			throws CannotCompileException, NotFoundException {
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
				try {
					m = interfaze.getMethod(method.getName(),
							method.getSignature());
				} catch (final NotFoundException e) {
				}
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

	public static boolean methodExistsInClass(final CtClass clazz,
			final String methodName, final String signature) {
		for (final CtMethod method : clazz.getMethods()) {
			if (method.getName().equals(methodName)
					&& method.getSignature().equals(signature)) {
				return true;
			}
		}
		return false;
	}

	public static boolean fieldExistsInClass(final CtClass clazz,
			final String fieldName) {
		for (final CtField field : clazz.getFields()) {
			if (field.getName().equals(fieldName)) {
				return true;
			}
		}
		return false;
	}

	public static List<CtClass> getParents(final CtClass targetClass)
			throws NotFoundException {
		final List<CtClass> parents = new LinkedList<CtClass>();
		CtClass parent = targetClass.getSuperclass();
		while (!parent.getName().equals("java.lang.Object")) {
			parents.add(parent);
			parent = parent.getSuperclass();
		}
		return parents;
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
			final String errorClassName, final String errorMessage,
			final AssertionCodec... codecs) {
		final StringBuilder src = new StringBuilder();
		for (final String assertion : assertions) {
			src.append(String.format("if(!(%s)) throw new %s(\"%s\");",
					parseAssertion(assertion, codecs), errorClassName,
					errorMessage));
		}
		return src.toString();
	}

	private static String parseAssertion(final String assertion,
			final AssertionCodec... codecs) {
		String result = assertion;
		if (codecs != null) {
			for (final AssertionCodec codec : codecs) {
				result = codec.decode(result);
			}
		}
		return result;
	}
}
