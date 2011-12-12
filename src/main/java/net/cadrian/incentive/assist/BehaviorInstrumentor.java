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

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.compiler.CompileError;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;
import net.cadrian.incentive.error.EnsureError;
import net.cadrian.incentive.error.RequireError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BehaviorInstrumentor {
	private static final Logger LOG = LoggerFactory
			.getLogger(BehaviorInstrumentor.class);

	private static final String OLD_LOCAL_VAR = "__incentive_old__";

	@SuppressWarnings("boxing")
	static final String OLD_CLASS_NAME(final CtClass parent, final int index) {
		return String.format("%s.__incentive_%s_old%d__",
				parent.getPackageName(), parent.getSimpleName(), index);
	}

	private final String oldClassName;

	private static final String POSTCONDITION_ERROR_NAME = EnsureError.class
			.getName();
	private static final String PRECONDITION_ERROR_NAME = RequireError.class
			.getName();

	protected abstract String getPreconditionName();

	protected abstract String getPostconditionName();

	protected abstract CtClass getReturnType() throws NotFoundException;

	protected abstract CtBehavior getPrecursor() throws NotFoundException;

	protected abstract void insertClassInvariantCall(final boolean before)
			throws CannotCompileException, NotFoundException,
			ClassNotFoundException;

	protected abstract void insertBeforeBody(String a_code)
			throws CannotCompileException;

	protected abstract void setPreconditionModifiers(CtMethod a_precondition);

	protected final ClassInstrumentor classInstrumentor;
	private final CtBehavior behavior;
	private final ClassPool pool;
	protected final CtClass targetClass;

	final Instrumentor instrumentor;

	private final CtClass oldValuesClass;
	private CtMethod precondition;
	private CtMethod postcondition;

	public BehaviorInstrumentor(final ClassInstrumentor a_classInstrumentor,
			final CtBehavior a_behavior, final ClassPool a_pool,
			final int a_oldClassIndex) {
		this.classInstrumentor = a_classInstrumentor;
		this.instrumentor = a_classInstrumentor.instrumentor;
		this.behavior = a_behavior;
		this.pool = a_pool;
		this.targetClass = a_behavior.getDeclaringClass();
		this.oldClassName = OLD_CLASS_NAME(targetClass, a_oldClassIndex);
		this.oldValuesClass = targetClass.isFrozen() ? null : pool
				.makeClass(oldClassName);
	}

	void instrument() throws CannotCompileException, NotFoundException,
			ClassNotFoundException, CompileError, IOException {
		definePreconditionMethod();
		definePostconditionMethod();

		// NOTE! insert() adds code at the very start of the bytecode block;
		// hence insert the precondition check before the invariant check
		insertMethodPreconditionCall();
		insertClassInvariantCall(true);

		insertMethodPostconditionCall();
		insertClassInvariantCall(false);
	}

	private boolean insertMethodPreconditionCall()
			throws CannotCompileException {
		LOG.info("-- now adding precondition call to {}",
				behavior.getLongName());
		if (precondition == null
				|| InstrumentorUtil.instrumentedWith(behavior,
						precondition.getName(), precondition.getSignature())
				|| Modifier.isAbstract(behavior.getModifiers())) {
			LOG.info(" ** precondition not added to {}", behavior.getName());
			return false;
		}
		behavior.addLocalVariable(OLD_LOCAL_VAR, oldValuesClass);
		final String code = String.format("%s = %s($$);", OLD_LOCAL_VAR,
				precondition.getName());
		behavior.insertBefore(code);
		LOG.info(" ** added precondition call to {}: {}", behavior.getName(),
				code);
		return true;
	}

	public String makePostconditionSignature() throws NotFoundException {
		final CtClass returnType = getReturnType();
		final String voidDescriptor = InstrumentorUtil.voidify(behavior
				.getSignature());
		final String returnDescriptor = Descriptor.insertParameter(
				returnType == CtClass.voidType ? CtClass.intType : returnType,
				voidDescriptor);
		return Descriptor.insertParameter(oldClassName, returnDescriptor);
	}

	private boolean insertMethodPostconditionCall()
			throws CannotCompileException, NotFoundException {
		LOG.info("-- now adding postcondition call to {}",
				behavior.getLongName());
		if (postcondition == null
				|| InstrumentorUtil.instrumentedWith(behavior,
						postcondition.getName(), postcondition.getSignature())
				|| Modifier.isAbstract(behavior.getModifiers())) {
			LOG.info(" ** postcondition not added to {}", behavior.getName());
			return false;
		}
		final String code = String.format("%s(%s,%s,$$);", postcondition
				.getName(), precondition == null ? "null" : OLD_LOCAL_VAR,
				getReturnType() == CtClass.voidType ? "0" : "$_");
		behavior.insertAfter(code);
		LOG.info(" ** added postcondition call to {}: {}", behavior.getName(),
				code);
		return true;
	}

	private void definePreconditionMethod() throws CannotCompileException,
			NotFoundException, ClassNotFoundException, CompileError,
			IOException {
		final StringBuilder src = new StringBuilder(String.format(
				"{%s err=null;\n", RequireError.class.getName()));
		fillPreconditionClass();
		src.append(String.format("final %s result = new %s();\n", oldClassName,
				oldClassName));
		addPreconditionOld(src);
		addPreconditionCode(src);
		src.append("if(err!=null)throw err;\nreturn result;\n}");
		final String code = src.toString();
		precondition = CtNewMethod
				.make(oldValuesClass, getPreconditionName(),
						behavior.getParameterTypes(), new CtClass[0], code,
						targetClass);
		LOG.info("Precondition of {} is {}{}",
				new Object[] { behavior.getLongName(), precondition, code });
		setPreconditionModifiers(precondition);
		targetClass.addMethod(precondition);
	}

	private void fillPreconditionClass() throws ClassNotFoundException,
			NotFoundException, CannotCompileException, CompileError,
			IOException {
		oldValuesClass.setModifiers(Modifier.FINAL);
		addPreconditionClassFields(oldValuesClass);
		instrumentor.writeToCache(oldClassName, oldValuesClass.toBytecode());
		oldValuesClass.toClass(); // to load it in the JVM
	}

	private void addPreconditionClassFields(final CtClass a_preconditionClass)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException, CompileError {
		for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
			final BehaviorInstrumentor parentBehavior = parent
					.getBehavior(getKey());
			if (parentBehavior != null) {
				parentBehavior.addPreconditionClassFields(a_preconditionClass);
			}
		}

		final Ensure ensure = (Ensure) getPrecursor().getAnnotation(
				Ensure.class);
		if (ensure != null) {
			for (final String assertion : ensure.value()) {
				InstrumentorUtil
						.transform(
								assertion,
								targetClass,
								pool,
								TransformCodecs.PRECONDITION_ARGUMENTS_CODEC,
								TransformCodecs
										.PRECONDITION_OLD_CLASS_CODEC(a_preconditionClass));
			}
		}
	}

	private void addPreconditionOld(final StringBuilder src)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException, CompileError {
		for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
			final BehaviorInstrumentor parentBehavior = parent
					.getBehavior(getKey());
			if (parentBehavior != null) {
				parentBehavior.addPreconditionOld(src);
			}
		}

		final Ensure ensure = (Ensure) getPrecursor().getAnnotation(
				Ensure.class);
		if (ensure != null) {
			for (final String assertion : ensure.value()) {
				src.append(InstrumentorUtil.transform(assertion, targetClass,
						pool, TransformCodecs.PRECONDITION_ARGUMENTS_CODEC,
						TransformCodecs.PRECONDITION_OLD_VALUES_CODEC));
			}
		}
	}

	private boolean addPreconditionCode(final StringBuilder src)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException, CompileError {
		boolean result = false;
		for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
			final BehaviorInstrumentor parentBehavior = parent
					.getBehavior(getKey());
			if (parentBehavior != null) {
				final int srcLength = src.length();
				src.append("try{");
				final boolean hasRequire = parentBehavior
						.addPreconditionCode(src);
				if (hasRequire) {
					src.append(String.format(
							"return result;}\ncatch(%s x){err=x;}\n",
							PRECONDITION_ERROR_NAME));
				} else {
					src.setLength(srcLength);
				}
			}
		}

		final Require require = (Require) getPrecursor().getAnnotation(
				Require.class);
		if (require != null) {
			src.append(InstrumentorUtil.parseAssertions(require.value(),
					targetClass, pool, PRECONDITION_ERROR_NAME, getName(),
					TransformCodecs.PRECONDITION_ARGUMENTS_CODEC));
			result = true;
		}

		return result;
	}

	private String getName() {
		return behavior.getLongName();
	}

	private void definePostconditionMethod() throws CannotCompileException,
			NotFoundException, ClassNotFoundException, CompileError {
		final StringBuilder src = new StringBuilder("{");
		addPostconditionCode(src);
		src.append('}');
		final CtClass[] params;
		final CtClass[] params0 = behavior.getParameterTypes();
		if (params0 == null) {
			params = new CtClass[2];
		} else {
			params = new CtClass[params0.length + 2];
			System.arraycopy(params0, 0, params, 2, params0.length);
		}
		params[0] = oldValuesClass;
		params[1] = getReturnType();
		if (params[1] == CtClass.voidType) {
			params[1] = CtClass.intType;
		}

		final String code = src.toString();
		postcondition = CtNewMethod.make(CtClass.voidType,
				getPostconditionName(), params, new CtClass[0], code,
				targetClass);
		LOG.info("Postcondition of {} is {}{}",
				new Object[] { behavior.getLongName(), postcondition, code });
		targetClass.addMethod(postcondition);
	}

	private void addPostconditionCode(final StringBuilder src)
			throws ClassNotFoundException, NotFoundException,
			CannotCompileException, CompileError {
		for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
			final BehaviorInstrumentor parentBehavior = parent
					.getBehavior(getKey());
			if (parentBehavior != null) {
				parentBehavior.addPostconditionCode(src);
			}
		}
		final Ensure ensure = (Ensure) getPrecursor().getAnnotation(
				Ensure.class);
		if (ensure != null) {
			src.append(InstrumentorUtil.parseAssertions(ensure.value(),
					targetClass, pool, POSTCONDITION_ERROR_NAME, getName(),
					TransformCodecs.POSTCONDITION_RESULT_CODEC,
					TransformCodecs.POSTCONDITION_ARGUMENTS_CODEC,
					TransformCodecs.POSTCONDITION_OLD_VALUES_CODEC));
		}
	}

	public String getKey() {
		return behavior.getName() + ":" + behavior.getSignature();
	}

}
