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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.Require;
import net.cadrian.incentive.error.EnsureError;
import net.cadrian.incentive.error.RequireError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BehaviorInstrumentor {
	private static final Logger LOG = LoggerFactory
			.getLogger(BehaviorInstrumentor.class);

	static final String OLD_LOCAL_VAR = "__incentive_old__";
	private static final String OLD_LOCAL_TYPE = "java.util.ArrayList";

	private static final String POSTCONDITION_ERROR_NAME = EnsureError.class
			.getName();
	private static final String PRECONDITION_ERROR_NAME = RequireError.class
			.getName();

	protected abstract String getPreconditionName();

	protected abstract String getPostconditionName();

	protected abstract CtClass getReturnType() throws NotFoundException;

	protected abstract void addClassInvariantCall(final boolean before)
			throws CannotCompileException, NotFoundException,
			ClassNotFoundException;

	protected final ClassInstrumentor classInstrumentor;
	private final CtBehavior behavior;
	private final ClassPool pool;
	private final CtClass targetClass;

	public BehaviorInstrumentor(final ClassInstrumentor a_classInstrumentor,
			final CtBehavior a_behavior, final ClassPool a_pool) {
		this.classInstrumentor = a_classInstrumentor;
		this.behavior = a_behavior;
		this.pool = a_pool;
		this.targetClass = a_behavior.getDeclaringClass();
	}

	void instrument() throws CannotCompileException, NotFoundException,
			ClassNotFoundException {
		addPreconditionMethod();
		addPostconditionMethod();
		addClassInvariantCall(true);
		addMethodPreconditionCall();
		addMethodPostconditionCall();
		addClassInvariantCall(false);
	}

	public String makePreconditionSignature() {
		return Descriptor.changeReturnType(OLD_LOCAL_TYPE,
				behavior.getSignature());
	}

	private boolean addMethodPreconditionCall() throws CannotCompileException {
		final String req = getPreconditionName();
		try {
			final String preconditionSignature = makePreconditionSignature();
			targetClass.getMethod(req, preconditionSignature);
			if (InstrumentorUtil.instrumentedWith(behavior, req,
					preconditionSignature)
					|| Modifier.isAbstract(behavior.getModifiers())) {
				return false;
			}
		} catch (final NotFoundException x) {
			return false;
		}
		behavior.insertBefore(String.format("final %s = %s($$);",
				OLD_LOCAL_VAR, req));
		return true;
	}

	public String makePostconditionSignature() throws NotFoundException {
		final CtClass returnType = getReturnType();
		final String voidDescriptor = InstrumentorUtil.voidify(behavior
				.getSignature());
		final String returnDescriptor = returnType == CtClass.voidType ? voidDescriptor
				: Descriptor.insertParameter(returnType, voidDescriptor);
		return Descriptor.insertParameter(OLD_LOCAL_TYPE, returnDescriptor);
	}

	private boolean addMethodPostconditionCall() throws CannotCompileException,
			NotFoundException {
		final String ens = getPostconditionName();
		try {
			final String postconditionSignature = makePostconditionSignature();
			targetClass.getMethod(ens, postconditionSignature);
			if (InstrumentorUtil.instrumentedWith(behavior, ens,
					postconditionSignature)
					|| Modifier.isAbstract(behavior.getModifiers())) {
				return false;
			}
		} catch (final NotFoundException x) {
			return false;
		}
		behavior.insertAfter(String.format("%s(%s%s,$$);", ens, OLD_LOCAL_VAR,
				getReturnType() == CtClass.voidType ? "" : ",$_"));
		return true;
	}

	private void addPreconditionMethod() throws CannotCompileException,
			NotFoundException, ClassNotFoundException {
		final StringBuilder src = new StringBuilder("{").append(
				RequireError.class.getName()).append(" err=null;");
		addPreconditionCode(src);
		src.append("if(err!=null)throw err;}");
		final String code = src.toString();
		LOG.info("Adding {}: {}", getPreconditionName(), code);
		targetClass.addMethod(CtNewMethod.make(CtClass.voidType,
				getPreconditionName(), behavior.getParameterTypes(),
				new CtClass[0], code, targetClass));
	}

	private void addPreconditionCode(final StringBuilder src)
			throws ClassNotFoundException {
		for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
			final BehaviorInstrumentor parentBehavior = parent
					.getBehavior(getKey());
			if (parentBehavior != null) {
				src.append("try{");
				parentBehavior.addPreconditionCode(src);
				src.append(String.format(
						"return;}catch(%s x){err=new %s(\"%s\",x);}",
						PRECONDITION_ERROR_NAME, PRECONDITION_ERROR_NAME,
						parentBehavior.getName()));
			}
		}

		final Require require = (Require) targetClass
				.getAnnotation(Require.class);
		if (require != null) {
			src.append(InstrumentorUtil.parseAssertions(require.value(),
					PRECONDITION_ERROR_NAME, getName(),
					TransformCodecs.PRECONDITION_ARGUMENTS_CODEC));
		}

		final Ensure ensure = (Ensure) targetClass.getAnnotation(Ensure.class);
		if (ensure != null) {
			for (final String assertion : ensure.value()) {
				src.append(InstrumentorUtil.transform(assertion,
						TransformCodecs.PRECONDITION_ARGUMENTS_CODEC,
						TransformCodecs.PRECONDITION_OLD_VALUES_CODEC));
			}
		}
	}

	private String getName() {
		return behavior.getLongName();
	}

	private void addPostconditionMethod() throws CannotCompileException,
			NotFoundException, ClassNotFoundException {
		final StringBuilder src = new StringBuilder("{");
		addPostconditionCode(src);
		src.append('}');
		final CtClass[] params;
		final CtClass[] params0 = behavior.getParameterTypes();
		final int extraParamCount = getReturnType() == CtClass.voidType ? 1 : 2;
		if (params0 == null) {
			params = new CtClass[extraParamCount];
		} else {
			params = new CtClass[params0.length + extraParamCount];
			System.arraycopy(params0, 0, params, extraParamCount,
					params0.length);
		}
		params[0] = pool.get(OLD_LOCAL_TYPE);
		if (extraParamCount == 2) {
			params[1] = getReturnType();
		}

		final String code = src.toString();
		LOG.info("Postcondition of {} is {}", behavior.getLongName(), code);
		targetClass.addMethod(CtNewMethod.make(CtClass.voidType,
				getPostconditionName(), params, new CtClass[0], code,
				targetClass));
	}

	private void addPostconditionCode(final StringBuilder src)
			throws ClassNotFoundException {
		for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
			final BehaviorInstrumentor parentBehavior = parent
					.getBehavior(getKey());
			if (parentBehavior != null) {
				parentBehavior.addPostconditionCode(src);
			}
		}
		final Ensure ensure = (Ensure) targetClass.getAnnotation(Ensure.class);
		if (ensure != null) {
			src.append(InstrumentorUtil.parseAssertions(ensure.value(),
					POSTCONDITION_ERROR_NAME, getName(),
					TransformCodecs.POSTCONDITION_RESULT_CODEC,
					TransformCodecs.POSTCONDITION_ARGUMENTS_CODEC,
					TransformCodecs.POSTCONDITION_OLD_VALUES_CODEC));
		}
	}

	public String getKey() {
		return behavior.getName() + ":" + behavior.getSignature();
	}

}
