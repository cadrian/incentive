package net.cadrian.incentive.assist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

class MethodInstrumentor extends BehaviourInstrumentor {
	private static final String PRECONDITION_NAME(final String name) {
		return String.format("__incentive_req_%s__", name);
	}

	private static final String POSTCONDITION_NAME(final String name) {
		return String.format("__incentive_ens_%s__", name);
	}

	private final CtMethod method;

	public MethodInstrumentor(final ClassInstrumentor a_classInstrumentor,
			final CtMethod a_targetMethod, final ClassPool a_pool) {
		super(a_classInstrumentor, a_targetMethod, a_pool);
		this.method = a_targetMethod;
	}

	@Override
	protected String getPreconditionName() {
		return PRECONDITION_NAME(method.getName());
	}

	@Override
	protected String getPostconditionName() {
		return POSTCONDITION_NAME(method.getName());
	}

	@Override
	protected CtClass getReturnType() throws NotFoundException {
		return method.getReturnType();
	}

	@Override
	protected void addClassInvariantCall(final boolean before)
			throws CannotCompileException, NotFoundException {
		if (before || !InstrumentorUtil.isPure(method)) {
			classInstrumentor.addClassInvariantCall(method, before, false);
		}
	}

}
