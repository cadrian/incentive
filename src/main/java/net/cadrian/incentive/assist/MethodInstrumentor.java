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
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

class MethodInstrumentor extends BehaviorInstrumentor {
	private static final String PRECONDITION_NAME(final String name) {
		return String.format("__incentive_req_%s__", name);
	}

	private static final String POSTCONDITION_NAME(final String name) {
		return String.format("__incentive_ens_%s__", name);
	}

	private final CtMethod method;

	public MethodInstrumentor(final ClassInstrumentor a_classInstrumentor,
			final CtMethod a_targetMethod, final ClassPool a_pool,
			final int oldClassIndex) {
		super(a_classInstrumentor, a_targetMethod, a_pool, oldClassIndex);
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
	protected void insertClassInvariantCall(final boolean before)
			throws CannotCompileException, NotFoundException,
			ClassNotFoundException {
		if (before || !InstrumentorUtil.isPure(method)) {
			classInstrumentor.addClassInvariantCall(method, before, false);
		}
	}

	@Override
	protected CtBehavior getPrecursor() throws NotFoundException {
		return targetClass.getMethod(method.getName(), method.getSignature());
	}

	@Override
	protected void insertBeforeBody(final String a_code)
			throws CannotCompileException {
		method.insertBefore(a_code);
	}

	@Override
	protected void setPreconditionModifiers(final CtMethod a_precondition) {
		a_precondition.setModifiers(Modifier.FINAL);
	}

}
