/*
 * Incentive, A Design By Contract framework for Java.
 * Copyright (C) 2011 Cyril Adrian. All Rights Reserved.
 *
 * Javaassist implementation based on C4J's
 * Copyright (C) 2006 Jonas Bergstr�m. All Rights Reserved.
 *
 * The contents of this file may be used under the terms of the GNU Lesser
 * General Public License Version 3.
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
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

class ConstructorInstrumentor extends BehaviorInstrumentor {
    @SuppressWarnings("boxing")
    private static final String PRECONDITION_NAME(final int index) {
        return String.format("__incentive_req%d__", index);
    }

    @SuppressWarnings("boxing")
    private static final String POSTCONDITION_NAME(final int index) {
        return String.format("__incentive_ens%d__", index);
    }

    private final int index;
    private final CtConstructor constructor;

    public ConstructorInstrumentor(final ClassInstrumentor a_classInstrumentor,
                                   final CtConstructor a_constructor, final int a_index,
                                   final ClassPool a_pool, final int oldClassIndex)
        throws ClassNotFoundException, NotFoundException {
        super(a_classInstrumentor, a_constructor, a_pool, oldClassIndex);
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
    protected void insertClassInvariantCall(final boolean before)
        throws CannotCompileException, NotFoundException {
        if (!before) {
            classInstrumentor.addClassInvariantCall(constructor, false, true);
        }
    }

    @Override
    protected CtBehavior getPrecursor() throws NotFoundException {
        return targetClass.getConstructor(constructor.getSignature());
    }

    @Override
    protected void setPreconditionModifiers(final CtMethod a_precondition) {
        a_precondition.setModifiers(Modifier.FINAL | Modifier.STATIC);
    }

}
