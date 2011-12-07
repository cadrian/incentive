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

abstract class BehaviorInstrumentor {
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
			throws CannotCompileException, NotFoundException;

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

	private void addPreconditionMethod() throws CannotCompileException,
			NotFoundException, ClassNotFoundException {
		final StringBuilder src = new StringBuilder("{").append(
				RequireError.class.getName()).append(" err=null;");
		addPreconditionCode(src);
		src.append("if(err!=null)throw err;}");
		targetClass.addMethod(CtNewMethod.make(CtClass.voidType,
				getPreconditionName(), behavior.getParameterTypes(),
				new CtClass[0], src.toString(), targetClass));
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
						"return;}catch(%s){err=new %s(\"%s\",err);}",
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
		final StringBuilder src = new StringBuilder('{');
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
		params[0] = pool.get(OLD_LOCAL_TYPE);
		params[1] = getReturnType();
		targetClass.addMethod(CtNewMethod.make(CtClass.voidType,
				getPostconditionName(), params, new CtClass[0], src.toString(),
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
