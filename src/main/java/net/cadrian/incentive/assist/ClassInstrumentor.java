package net.cadrian.incentive.assist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import net.cadrian.incentive.Invariant;
import net.cadrian.incentive.error.InvariantError;

class ClassInstrumentor {

	private static final String INITIALIZED_FLAG_VAR = "__incentive_initialized__";
	private static final String INVARIANT_METHOD_SIGNATURE = "()V";
	private static final String INVARIANT_METHOD_NAME = "__incentive_inv__";
	private static final String INVARIANT_ERROR_NAME = InvariantError.class
			.getName();

	private final CtClass targetClass;
	private final ClassPool pool;
	private final List<ClassInstrumentor> parents;
	private final List<MethodInstrumentor> methods;
	private final List<ConstructorInstrumentor> constructors;
	private final Map<String, BehaviorInstrumentor> behaviors;

	public ClassInstrumentor(final CtClass targetClass, final ClassPool pool)
			throws NotFoundException {
		this.targetClass = targetClass;
		this.pool = pool;

		parents = new ArrayList<ClassInstrumentor>();
		for (final CtClass parent : InstrumentorUtil.getParents(targetClass)) {
			parents.add(new ClassInstrumentor(parent, pool));
		}

		behaviors = new HashMap<String, BehaviorInstrumentor>();

		methods = new ArrayList<MethodInstrumentor>();
		for (final CtMethod targetMethod : targetClass.getDeclaredMethods()) {
			final MethodInstrumentor methodInstrumentor = new MethodInstrumentor(
					this, targetMethod, pool);
			methods.add(methodInstrumentor);
			behaviors.put(methodInstrumentor.getKey(), methodInstrumentor);
		}

		constructors = new ArrayList<ConstructorInstrumentor>();
		int i = 0;
		for (final CtConstructor constructor : targetClass.getConstructors()) {
			final ConstructorInstrumentor constructorInstrumentor = new ConstructorInstrumentor(
					this, constructor, i++, pool);
			constructors.add(constructorInstrumentor);
			behaviors.put(constructorInstrumentor.getKey(),
					constructorInstrumentor);
		}
	}

	public List<ClassInstrumentor> getParents() {
		return parents;
	}

	public List<MethodInstrumentor> getMethods() {
		return methods;
	}

	public List<ConstructorInstrumentor> getConstructors() {
		return constructors;
	}

	public BehaviorInstrumentor getBehavior(final String key) {
		return behaviors.get(key);
	}

	void instrument() throws CannotCompileException, NotFoundException,
			ClassNotFoundException {
		for (final ClassInstrumentor parent : parents) {
			parent.instrument();
		}
		addInitializedFlag();
		addInvariantMethod();
		for (final ConstructorInstrumentor constructor : constructors) {
			constructor.instrument();
		}
		for (final MethodInstrumentor method : methods) {
			method.instrument();
		}
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

	boolean addClassInvariantCall(final CtBehavior a_behavior,
			final boolean before, final boolean initialized)
			throws CannotCompileException, NotFoundException {

		final CtClass targetClass = a_behavior.getDeclaringClass();
		if (!InstrumentorUtil.methodExistsInClass(targetClass,
				INVARIANT_METHOD_NAME, INVARIANT_METHOD_SIGNATURE)
				|| InstrumentorUtil.instrumentedWith(a_behavior,
						INVARIANT_METHOD_NAME, INVARIANT_METHOD_SIGNATURE)
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
				INITIALIZED_FLAG_VAR, INVARIANT_METHOD_NAME);
		if (before) {
			a_behavior.insertBefore(invCall);
		} else {
			a_behavior.insertAfter(invCall);
		}
		return true;
	}

	private void addInvariantMethod() throws CannotCompileException,
			NotFoundException, ClassNotFoundException {
		final StringBuilder src = new StringBuilder(String.format(
				"private void %s() {", INVARIANT_METHOD_NAME));
		addInvariantCode(src);
		src.append('}');
		targetClass.addMethod(CtNewMethod.make(src.toString(), targetClass));
	}

	private void addInvariantCode(final StringBuilder src)
			throws ClassNotFoundException {
		for (final ClassInstrumentor parent : parents) {
			parent.addInvariantCode(src);
		}
		final Invariant invariant = (Invariant) targetClass
				.getAnnotation(Invariant.class);
		if (invariant != null) {
			src.append(InstrumentorUtil.parseAssertions(invariant.value(),
					INVARIANT_ERROR_NAME, getName()));
		}
	}

	public String getName() {
		return targetClass.getName();
	}

}
