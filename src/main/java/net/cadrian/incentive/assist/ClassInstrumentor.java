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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClassInstrumentor {
	private static final Logger LOG = LoggerFactory
			.getLogger(ClassInstrumentor.class);

	private static final String INITIALIZED_FLAG_VAR = "__incentive_initialized__";
	private static final String INVARIANT_FLAG_VAR = "__incentive_invariant__";
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

		if (targetClass.isInterface()) {
			// We're only instrumenting classes
			LOG.info("{} is an interface: not instrumented",
					targetClass.getName());
			return;
		}

		if (!InstrumentorUtil.hasDBC(targetClass)) {
			// DBC annotation absent or skip=true
			LOG.info("{} has no DBC, or skipped", targetClass.getName());
			return;
		}

		addPrivateFlag(INITIALIZED_FLAG_VAR);
		addPrivateFlag(INVARIANT_FLAG_VAR);
		addInvariantMethod();
		for (final ConstructorInstrumentor constructor : constructors) {
			constructor.instrument();
		}
		for (final MethodInstrumentor method : methods) {
			method.instrument();
		}
	}

	private void addPrivateFlag(final String flagName)
			throws CannotCompileException {
		try {
			targetClass.getField(flagName);
		} catch (final NotFoundException x) {
			final String code = String.format("private boolean %s=false;",
					flagName);
			LOG.info("Adding to {}: {}", targetClass.getName(), code);
			final CtField flagField = CtField.make(code, targetClass);
			targetClass.addField(flagField);
		}
	}

	boolean addClassInvariantCall(final CtBehavior a_behavior,
			final boolean before, final boolean initialized)
			throws CannotCompileException {

		try {
			targetClass.getMethod(INVARIANT_METHOD_NAME,
					INVARIANT_METHOD_SIGNATURE);
			if (InstrumentorUtil.instrumentedWith(a_behavior,
					INVARIANT_METHOD_NAME, INVARIANT_METHOD_SIGNATURE)
					|| a_behavior.getDeclaringClass().equals(
							pool.get("java.lang.Object"))
					|| Modifier.isAbstract(a_behavior.getModifiers())
					|| Modifier.isStatic(a_behavior.getModifiers())) {
				return false;
			}
		} catch (final NotFoundException x) {
			return false;
		}

		final String code;
		if (initialized) {
			// true for constructors; in that case, `before' is false
			// Note that in that case, the invariant flag is obviously false.
			a_behavior.insertAfter(String.format("%s=true;",
					INITIALIZED_FLAG_VAR));
			code = String.format("try{%s=true;%s();}finally{%s=false;}",
					INVARIANT_FLAG_VAR, INVARIANT_METHOD_NAME,
					INVARIANT_FLAG_VAR);
		} else {
			// Only verify invariant if the instance has been fully created,
			// hence the "if(...)"
			code = String.format(
					"if(%s&&!%s){try{%s=true;%s();}finally{%s=false;}}",
					INITIALIZED_FLAG_VAR, INVARIANT_FLAG_VAR,
					INVARIANT_FLAG_VAR, INVARIANT_METHOD_NAME,
					INVARIANT_FLAG_VAR);
		}

		LOG.info("Adding {} {}: {}", new String[] {
				before ? "before" : "after", a_behavior.getName(), code });

		if (before) {
			a_behavior.insertBefore(code);
		} else {
			a_behavior.insertAfter(code);
		}
		return true;
	}

	private void addInvariantMethod() throws CannotCompileException,
			ClassNotFoundException {
		final StringBuilder src = new StringBuilder(String.format(
				"private void %s() {", INVARIANT_METHOD_NAME));
		addInvariantCode(src);
		src.append('}');
		final String code = src.toString();
		LOG.info("Adding to {}: {}", targetClass.getName(), code);
		targetClass.addMethod(CtNewMethod.make(code, targetClass));
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
