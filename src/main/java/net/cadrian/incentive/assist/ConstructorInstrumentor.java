package net.cadrian.incentive.assist;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

class ConstructorInstrumentor extends BehaviorInstrumentor {
	private static final String PRECONDITION_NAME(final int index) {
		return String.format("__incentive_req%d__", index);
	}

	private static final String POSTCONDITION_NAME(final int index) {
		return String.format("__incentive_ens%d__", index);
	}

	private final int index;
	private final CtConstructor constructor;

	public ConstructorInstrumentor(final ClassInstrumentor a_classInstrumentor,
			final CtConstructor a_constructor, final int a_index,
			final ClassPool a_pool) {
		super(a_classInstrumentor, a_constructor, a_pool);
		this.constructor = a_constructor;
		this.index = a_index;
	}

	@Override
	protected String getPreconditionName() {
		return PRECONDITION_NAME(index);
	}

	@Override
	protected String getPostconditionName() {
		return POSTCONDITION_NAME(index);
	}

	@Override
	protected CtClass getReturnType() {
		return CtClass.voidType;
	}

	@Override
	protected void addClassInvariantCall(final boolean before)
			throws CannotCompileException, NotFoundException {
		if (!before) {
			classInstrumentor.addClassInvariantCall(constructor, false, true);
		}
	}

}
