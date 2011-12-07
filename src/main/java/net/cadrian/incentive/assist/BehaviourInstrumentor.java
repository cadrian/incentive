package net.cadrian.incentive.assist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

abstract class BehaviourInstrumentor {
	private static final String OLD_LOCAL_VAR = "__incentive_old__";
	private static final String OLD_LOCAL_TYPE = "java.util.ArrayList";

	protected abstract String getPreconditionName();

	protected abstract String getPostconditionName();

	protected abstract CtClass getReturnType() throws NotFoundException;

	protected abstract void addClassInvariantCall(final boolean before)
			throws CannotCompileException, NotFoundException;

	protected final ClassInstrumentor classInstrumentor;
	private final CtBehavior behavior;
	private final ClassPool pool;

	public BehaviourInstrumentor(final ClassInstrumentor a_classInstrumentor,
			final CtBehavior a_behavior, final ClassPool a_pool) {
		this.classInstrumentor = a_classInstrumentor;
		this.behavior = a_behavior;
		this.pool = a_pool;
	}

	void instrument() throws CannotCompileException, NotFoundException {
		addPreconditionMethod();
		addPostconditionMethod();
		addClassInvariantCall(true);
		addMethodPreconditionCall();
		addMethodPostconditionCall();
		addClassInvariantCall(false);
	}

	public String makePreconditionSignature() throws NotFoundException {
		final String oldListDescriptor = Descriptor
				.of(pool.get(OLD_LOCAL_TYPE));
		return Descriptor.changeReturnType(oldListDescriptor,
				behavior.getSignature());
	}

	private boolean addMethodPreconditionCall() throws CannotCompileException,
			NotFoundException {
		final String req = getPreconditionName();
		final String preconditionSignature = makePreconditionSignature();
		final CtClass targetClass = behavior.getDeclaringClass();
		if (!InstrumentorUtil.methodExistsInClass(targetClass, req,
				preconditionSignature)
				|| InstrumentorUtil.instrumentedWith(behavior, req,
						preconditionSignature)
				|| Modifier.isAbstract(behavior.getModifiers())) {
			return false;
		}
		behavior.insertBefore(String.format("final %s = %s($$);",
				OLD_LOCAL_VAR, req));
		return true;
	}

	public String makePostconditionSignature() throws NotFoundException {
		final CtClass returnType = getReturnType();
		final String voidDescriptor = Descriptor.changeReturnType("V",
				behavior.getSignature());
		final String returnDescriptor = returnType == CtClass.voidType ? voidDescriptor
				: Descriptor.insertParameter(returnType, voidDescriptor);
		final String oldListDescriptor = Descriptor
				.of(pool.get(OLD_LOCAL_TYPE));
		return Descriptor.insertParameter(oldListDescriptor, returnDescriptor);
	}

	private boolean addMethodPostconditionCall() throws CannotCompileException,
			NotFoundException {
		final String ens = getPostconditionName();
		final CtClass targetClass = behavior.getDeclaringClass();
		final String postconditionSignature = makePostconditionSignature();
		if (!InstrumentorUtil.methodExistsInClass(targetClass, ens,
				postconditionSignature)
				|| InstrumentorUtil.instrumentedWith(behavior, ens,
						postconditionSignature)
				|| Modifier.isAbstract(behavior.getModifiers())) {
			return false;
		}
		behavior.insertAfter(String.format("%s(%s%s,$$);", ens, OLD_LOCAL_VAR,
				getReturnType() == CtClass.voidType ? "" : ",$_"));
		return true;
	}

	private void addPreconditionMethod() {
		// TODO Auto-generated method stub

	}

	private void addPostconditionMethod() {
		// TODO Auto-generated method stub

	}

}
