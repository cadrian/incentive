package net.cadrian.incentive.assist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

class ClassInstrumentor {

	private static final String INITIALIZED_FLAG_VAR = "__incentive_initialized__";

	private static final String INVARIANT_METHOD_SIGNATURE = "()V";

	private static final String INVARIANT_METHOD_NAME(final String name) {
		return String.format("__incentive_inv_%s__", name);
	}

	private final CtClass targetClass;
	private final ClassPool pool;

	public ClassInstrumentor(final CtClass targetClass, final ClassPool pool) {
		this.targetClass = targetClass;
		this.pool = pool;
	}

	void instrument() throws CannotCompileException, NotFoundException {
		addInitializedFlag();
		addInvariantMethod();
		instrumentConstructors();
		instrumentMethods();
	}

	private void addInitializedFlag() throws CannotCompileException {
		if (!InstrumentorUtil.fieldExistsInClass(targetClass,
				INITIALIZED_FLAG_VAR)) {
			final CtField initializedFlag = CtField.make(String.format(
					"private boolean %s=false;", INITIALIZED_FLAG_VAR),
					targetClass);
			targetClass.addField(initializedFlag);
		}
	}

	private void instrumentMethods() throws CannotCompileException,
			NotFoundException {
		final CtMethod[] targetMethods = targetClass.getDeclaredMethods();
		for (final CtMethod targetMethod : targetMethods) {
			new MethodInstrumentor(this, targetMethod, pool).instrument();
		}
	}

	private void instrumentConstructors() throws CannotCompileException,
			NotFoundException {
		final CtConstructor[] constructors = targetClass.getConstructors();
		int i = 0;
		for (final CtConstructor constructor : constructors) {
			new ConstructorInstrumentor(this, constructor, i++, pool)
					.instrument();
		}
	}

	boolean addClassInvariantCall(final CtBehavior a_behavior,
			final boolean before, final boolean initialized)
			throws CannotCompileException, NotFoundException {

		final CtClass targetClass = a_behavior.getDeclaringClass();
		final String inv = INVARIANT_METHOD_NAME(targetClass.getSimpleName());
		if (!InstrumentorUtil.methodExistsInClass(targetClass, inv,
				INVARIANT_METHOD_SIGNATURE)
				|| InstrumentorUtil.instrumentedWith(a_behavior, inv,
						INVARIANT_METHOD_SIGNATURE)
				|| a_behavior.getDeclaringClass().equals(
						pool.get("java.lang.Object"))
				|| Modifier.isAbstract(a_behavior.getModifiers())
				|| Modifier.isStatic(a_behavior.getModifiers())) {
			return false;
		}

		if (initialized) {
			// true for constructors; in that case, `before' is false
			a_behavior.insertAfter(String.format("%s=true;",
					INITIALIZED_FLAG_VAR));
		}

		// Only verify invariant if the instance has been fully created,
		// hence the "if(...)"
		final String invCall = String.format("if(%s)%s();",
				INITIALIZED_FLAG_VAR, inv);
		if (before) {
			a_behavior.insertBefore(invCall);
		} else {
			a_behavior.insertAfter(invCall);
		}
		return true;
	}

	private void addInvariantMethod() {
		// TODO Auto-generated method stub

	}

}
