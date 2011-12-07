package net.cadrian.incentive.assist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
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
		class Tmp {
			boolean b = false;
		}
		;
		final Tmp tmp = new Tmp();
		method.instrument(new ExprEditor() {
			@Override
			public void edit(final MethodCall m) {
				if (m.getMethodName().equals(methodName)
						&& m.getSignature().equals(signature)) {
					tmp.b = true;
				}
			}
		});
		return tmp.b;
	}

	public static String getSetReturnValueMethodName(final CtBehavior behavior)
			throws NotFoundException {
		final CtMethod[] methods = behavior.getDeclaringClass().getMethods();
		for (final CtMethod method : methods) {
			if (method.getName().equals(
					"___setReturnValuepost_" + behavior.getName()
							+ getParameterTypeStringShort(behavior))
					&& method.getSignature().equals("(Ljava/lang/Object;)V")) {
				return method.getName();
			}
		}
		return null;
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
			throws CannotCompileException {
		if (methodAnnotatedWithPure(method)) {
			return true;
		}

		try {
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
		} catch (final NotFoundException e) {
		}

		class Tmp {
			boolean fieldAccess = true;
			boolean other = true;
		}
		;
		final Tmp tmp = new Tmp();
		method.instrument(new ExprEditor() {
			@Override
			public void edit(final Cast c) {
				// Should be ok
			}

			@Override
			public void edit(final ConstructorCall c) {
				tmp.other = false;
			}

			@Override
			public void edit(final FieldAccess f) {
				tmp.fieldAccess = f.isReader() && tmp.fieldAccess;
			}

			@Override
			public void edit(final Handler h) {
				tmp.other = false;
			}

			@Override
			public void edit(final Instanceof i) {
				// Should be ok
			}

			@Override
			public void edit(final MethodCall m) {
				tmp.other = false;
			}

			@Override
			public void edit(final NewArray a) {
				tmp.other = false;
			}

			@Override
			public void edit(final NewExpr e) {
				tmp.other = false;
			}
		});
		return tmp.fieldAccess && tmp.other;
	}

	public static boolean preConditionExistsFor(final CtMethod postCondition,
			final List<CtMethod> preConditions) {
		final String preName = "pre_"
				+ postCondition.getName().substring(5,
						postCondition.getName().length());
		for (final CtMethod method : preConditions) {
			if (method.getName().equals(preName)
					&& method.getSignature().equals(
							postCondition.getSignature())) {
				return true;
			}
		}
		return false;
	}

	public static boolean methodExistsInClass(final CtClass clazz,
			final CtMethod method) {
		boolean methodExists = false;
		try {
			methodExists = clazz.getMethod(method.getName(),
					method.getSignature()) != null;
		} catch (final NotFoundException e) {
		}
		return methodExists;
	}

	public static boolean methodExistsInClass(final CtClass clazz,
			final String methodName, final String signature) {
		final CtMethod[] methods = clazz.getMethods();
		for (final CtMethod method : methods) {
			if (method.getName().equals(methodName)
					&& method.getSignature().equals(signature)) {
				return true;
			}
		}
		return false;
	}

	public static String makePreconditionSignature(final String signature,
			final ClassPool a_pool) throws NotFoundException {
		final String oldDescriptor = Descriptor
				.of(a_pool.get("java.util.List"));
		return Descriptor.changeReturnType(oldDescriptor, signature);
	}

	public static String makePostconditionSignature(final CtClass a_returnType,
			final String signature, final ClassPool a_pool)
			throws NotFoundException {
		final String voidDescriptor = Descriptor.changeReturnType("V",
				signature);
		final String returnDescriptor = Descriptor.insertParameter(
				a_returnType, voidDescriptor);
		final String oldDescriptor = Descriptor
				.of(a_pool.get("java.util.List"));
		return Descriptor.insertParameter(oldDescriptor, returnDescriptor);
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

	public static boolean superClassInstrumentedWith(final CtClass targetClass,
			final CtClass interfaceContract) throws NotFoundException {
		final List<CtClass> parents = getParents(targetClass);
		for (final CtClass parent : parents) {
			final CtField[] fields = parent.getDeclaredFields();
			for (final CtField field : fields) {
				if (field.getType().equals(interfaceContract)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean methodDeclaredInClass(final CtClass clazz,
			final CtMethod method) {
		boolean methodExists = false;
		try {
			methodExists = clazz.getDeclaredMethod(method.getName(),
					method.getParameterTypes()) != null;
		} catch (final NotFoundException e) {
		}
		return methodExists;
	}

	public static boolean methodDeclaredInClass(final CtClass clazz,
			final String methodName, final String signature) {
		final CtMethod[] methods = clazz.getDeclaredMethods();
		for (final CtMethod method : methods) {
			if (method.getName().equals(methodName)
					&& method.getSignature().equals(signature)) {
				return true;
			}
		}
		return false;
	}

	public static boolean postConditionExistsFor(final CtMethod preCondition,
			final List<CtMethod> postConditions) {
		final String postName = "post_"
				+ preCondition.getName().substring(4,
						preCondition.getName().length());
		for (final CtMethod method : postConditions) {
			if (method.getName().equals(postName)
					&& method.getSignature()
							.equals(preCondition.getSignature())) {
				return true;
			}
		}
		return false;
	}

	public static boolean oneArgConstructorExists(final CtClass contractClass) {
		for (final CtConstructor constructor : contractClass.getConstructors()) {
			try {
				if (constructor.getParameterTypes().length == 1) {
					return true;
				}
			} catch (final NotFoundException e) {
			}
		}
		return false;
	}

	public static CtMethod getSetOldValuesMethod(final CtClass targetClass)
			throws CannotCompileException {
		try {
			return targetClass.getMethod("___setOldValues", "()V");
		} catch (final NotFoundException e) {
			final CtMethod m = CtNewMethod.make(
					"private void ___setOldValues() {}", targetClass);
			targetClass.addMethod(m);
			return m;
		}
	}

	public static CtMethod getSetCurrentValuesMethod(final CtClass targetClass)
			throws CannotCompileException {
		try {
			return targetClass.getMethod("___setCurrentValues", "()V");
		} catch (final NotFoundException e) {
			final CtMethod m = CtNewMethod.make(
					"private void ___setCurrentValues() {}", targetClass);
			targetClass.addMethod(m);
			return m;
		}
	}

	public static boolean callsSuper(final CtClass targetClass,
			final String methodName, final CtClass[] parameterTypes)
			throws NotFoundException, CannotCompileException {
		for (final CtMethod method : targetClass.getMethods()) {
			if (method.getName().equals(methodName)
					&& Arrays
							.equals(method.getParameterTypes(), parameterTypes)) {
				return callsSuper(method);
			}
		}
		return false;
	}

	public static boolean callsSuper(final CtMethod method)
			throws CannotCompileException {
		class Tmp {
			boolean callsSuper = false;
		}
		;
		final Tmp tmp = new Tmp();
		method.instrument(new ExprEditor() {
			@Override
			public void edit(final Cast c) {
			}

			@Override
			public void edit(final ConstructorCall c) {
			}

			@Override
			public void edit(final FieldAccess f) {
			}

			@Override
			public void edit(final Handler h) {
			}

			@Override
			public void edit(final Instanceof i) {
			}

			@Override
			public void edit(final MethodCall methodCall) {
				if (!tmp.callsSuper) {
					final boolean super1 = methodCall.isSuper();
					boolean super2 = false;
					try {
						super2 = method.getDeclaringClass().subclassOf(
								methodCall.getMethod().getDeclaringClass());
					} catch (final NotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					final boolean equals = methodCall.getMethodName().equals(
							method.getName());
					final boolean equals2 = methodCall.getSignature().equals(
							method.getSignature());
					tmp.callsSuper = super1 && equals && equals2;
				}
			}

			@Override
			public void edit(final NewArray a) {
			}

			@Override
			public void edit(final NewExpr e) {
			}
		});
		return tmp.callsSuper;
	}

	public static String getParameterTypeStringShort(final CtBehavior behavior)
			throws NotFoundException {
		String s = "";
		final CtClass[] parameterTypes = behavior.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			s = s + "_" + parameterTypes[i].getSimpleName().replace("[]", "A");
		}
		return s;
	}

	public static boolean fieldExistsInClass(final CtClass targetClass,
			final String fieldName) {
		try {
			targetClass.getDeclaredField(fieldName);
			return true;
		} catch (final NotFoundException e) {
			return false;
		}
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
}
