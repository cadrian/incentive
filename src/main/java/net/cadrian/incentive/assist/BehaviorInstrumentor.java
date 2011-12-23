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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.compiler.CompileError;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import net.cadrian.incentive.assist.assertion.EnsureAssertion;
import net.cadrian.incentive.assist.assertion.RequireAssertion;
import net.cadrian.incentive.assist.visitor.CodeGenerator;
import net.cadrian.incentive.Ensure;
import net.cadrian.incentive.error.EnsureError;
import net.cadrian.incentive.error.RequireError;
import net.cadrian.incentive.Require;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BehaviorInstrumentor {
    private static final Logger LOG = LoggerFactory.getLogger(BehaviorInstrumentor.class);

    public static final String OLD_LOCAL_VAR = "__incentive_old__";

    @SuppressWarnings("boxing")
    static final String OLD_CLASS_NAME(final CtClass parent, final int index) {
        return String.format("%s.__incentive_%s_old%d__", parent.getPackageName(), parent.getSimpleName(), index);
    }

    private final String oldClassName;

    public static final String POSTCONDITION_ERROR_NAME = EnsureError.class.getName();
    public static final String PRECONDITION_ERROR_NAME = RequireError.class.getName();

    protected abstract String getPreconditionName();

    protected abstract String getPostconditionName();

    protected abstract CtClass getReturnType() throws NotFoundException;

    protected abstract CtBehavior getPrecursor() throws NotFoundException;

    protected abstract void insertClassInvariantCall(final boolean before) throws CannotCompileException, NotFoundException, ClassNotFoundException;

    protected abstract void setPreconditionModifiers(CtMethod a_precondition);

    protected final ClassInstrumentor classInstrumentor;
    public final CtBehavior behavior;
    public final ClassPool pool;
    public final CtClass targetClass;

    final Instrumentor instrumentor;

    public final CtClass oldValuesClass;
    public CtMethod precondition;
    public CtMethod postcondition;

    private final RequireAssertion preconditionAssertion;
    private final EnsureAssertion postconditionAssertion;

    public BehaviorInstrumentor(final ClassInstrumentor a_classInstrumentor, final CtBehavior a_behavior, final ClassPool a_pool, final int a_oldClassIndex)
        throws ClassNotFoundException, NotFoundException {
        this.classInstrumentor = a_classInstrumentor;
        this.instrumentor = a_classInstrumentor.instrumentor;
        this.behavior = a_behavior;
        this.pool = a_pool;
        this.targetClass = a_behavior.getDeclaringClass();

        this.oldClassName = OLD_CLASS_NAME(targetClass, a_oldClassIndex);
        if (targetClass.isFrozen()) {
            this.oldValuesClass = null;
        }
        else {
            final CtClass oldClass = Instrumentor.mainPool.getOrNull(oldClassName);
            if (oldClass == null) {
                this.oldValuesClass = Instrumentor.mainPool.makeClass(oldClassName);
            } else {
                this.oldValuesClass = oldClass;
            }
        }

        preconditionAssertion = new RequireAssertion(this);
        postconditionAssertion = new EnsureAssertion(this);
    }

    private void gatherPreconditions(final Set<CtClass> classes, final RequireAssertion preconditionAssertion) throws ClassNotFoundException, NotFoundException {
        if (classes.contains(targetClass)) return;
        classes.add(targetClass);

        for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
            final BehaviorInstrumentor parentBehavior = parent.getBehavior(getKey());
            if (parentBehavior != null) {
                parentBehavior.gatherPreconditions(classes, preconditionAssertion);
            }
        }

        final Require require = (Require) getPrecursor().getAnnotation(Require.class);
        if (require != null) {
            for (final String assertion: require.value()) {
                preconditionAssertion.add(targetClass, assertion);
            }
        }
    }

    private void gatherPostconditions(final Set<CtClass> classes, final EnsureAssertion postconditionAssertion) throws ClassNotFoundException, NotFoundException {
        if (classes.contains(targetClass)) return;
        classes.add(targetClass);

        for (final ClassInstrumentor parent : classInstrumentor.getParents()) {
            final BehaviorInstrumentor parentBehavior = parent.getBehavior(getKey());
            if (parentBehavior != null) {
                parentBehavior.gatherPostconditions(classes, postconditionAssertion);
            }
        }

        final Ensure ensure = (Ensure) getPrecursor().getAnnotation(Ensure.class);
        if (ensure != null) {
            for (final String assertion: ensure.value()) {
                postconditionAssertion.add(targetClass, assertion);
            }
        }
    }

    void instrument() throws CannotCompileException, NotFoundException, ClassNotFoundException, CompileError, IOException {
        gatherPreconditions(new HashSet<CtClass>(), preconditionAssertion);
        gatherPostconditions(new HashSet<CtClass>(), postconditionAssertion);

        definePreconditionMethod();
        definePostconditionMethod();

        // NOTE! insert() adds code at the very start of the bytecode block;
        // hence insert the precondition check before the invariant check
        insertMethodPreconditionCall();
        insertClassInvariantCall(true);

        insertMethodPostconditionCall();
        insertClassInvariantCall(false);
    }

    public String getName() {
        return behavior.getLongName();
    }

    public String getKey() {
        return behavior.getName() + ":" + behavior.getSignature();
    }

    private boolean insertMethodPreconditionCall() throws CannotCompileException {
        LOG.debug("-- now adding precondition call to {}", behavior.getLongName());
        if (precondition == null
            || InstrumentorUtil.instrumentedWith(behavior, precondition.getName(), precondition.getSignature())
            || Modifier.isAbstract(behavior.getModifiers())) {
            LOG.debug(" ** precondition not added to {}", behavior.getName());
            return false;
        }
        behavior.addLocalVariable(OLD_LOCAL_VAR, oldValuesClass);
        final String code = String.format("%s = %s($$);", OLD_LOCAL_VAR, precondition.getName());
        behavior.insertBefore(code);
        LOG.debug(" ** added precondition call to {}: {}", behavior.getName(), code);
        return true;
    }

//    public String makePostconditionSignature() throws NotFoundException {
//        final CtClass returnType = getReturnType();
//        final String voidDescriptor = InstrumentorUtil.voidify(behavior.getSignature());
//        final String returnDescriptor = Descriptor.insertParameter(returnType == CtClass.voidType ? CtClass.intType : returnType, voidDescriptor);
//        return Descriptor.insertParameter(oldClassName, returnDescriptor);
//    }

    private boolean insertMethodPostconditionCall() throws CannotCompileException, NotFoundException {
        LOG.debug("-- now adding postcondition call to {}", behavior.getLongName());
        if (postcondition == null
            || InstrumentorUtil.instrumentedWith(behavior, postcondition.getName(), postcondition.getSignature())
            || Modifier.isAbstract(behavior.getModifiers())) {
            LOG.debug(" ** postcondition not added to {}", behavior.getName());
            return false;
        }
        final String code = String.format("%s(%s,%s,$$);", postcondition.getName(), precondition == null ? "null" : OLD_LOCAL_VAR, getReturnType() == CtClass.voidType ? "null" : "$_");
        behavior.insertAfter(code);
        LOG.debug(" ** added postcondition call to {}: {}", behavior.getName(), code);
        return true;
    }

    private void definePreconditionMethod() throws CannotCompileException, NotFoundException, ClassNotFoundException, CompileError, IOException {
        LOG.info("Computing precondition of {}: {}", behavior.getLongName(), preconditionAssertion);
        LOG.info("with old expressions from postcondition of {}: {}", behavior.getLongName(), postconditionAssertion);
        final StringBuilder src = new StringBuilder("{\n");
        src.append("final ")
            .append(oldClassName)
            .append(" result = new ")
            .append(oldClassName)
            .append("();\n");
        src.append(CodeGenerator.ensureOld(classInstrumentor.generics, this, postconditionAssertion));
        commitPreconditionClass();
        src.append(CodeGenerator.require(classInstrumentor.generics, this, preconditionAssertion));
        src.append("return result;\n}");
        final String code = src.toString();
        try {
            precondition = CtNewMethod.make(oldValuesClass, getPreconditionName(), behavior.getParameterTypes(), new CtClass[0], code, targetClass);
            precondition.setModifiers(Modifier.PRIVATE);
            LOG.info("Precondition of {} is {}{}", new Object[]{behavior.getLongName(), precondition, code});
            setPreconditionModifiers(precondition);
            targetClass.addMethod(precondition);
        }
        catch (CannotCompileException ccx) {
            LOG.error(" *** CODE: {}", code, ccx);
            throw ccx;
        }
    }

    private void commitPreconditionClass() throws ClassNotFoundException, NotFoundException, CannotCompileException, CompileError, IOException {
        oldValuesClass.setModifiers(Modifier.FINAL);
        instrumentor.writeToCache(oldClassName, oldValuesClass.toBytecode());
        oldValuesClass.toClass(); // to load it in the JVM
    }

    private void definePostconditionMethod() throws CannotCompileException, NotFoundException, ClassNotFoundException, CompileError {
        LOG.info("Computing postcondition of {}: {}", behavior.getLongName(), postconditionAssertion);

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
            params[1] = pool.get("java.lang.Void");
        }

        final StringBuilder src = new StringBuilder("{\n");
        src.append(CodeGenerator.ensure(classInstrumentor.generics, this, postconditionAssertion));
        src.append("}\n");
        final String code = src.toString();
        try {
            postcondition = CtNewMethod.make(CtClass.voidType, getPostconditionName(), params, new CtClass[0], code, targetClass);
            postcondition.setModifiers(Modifier.PRIVATE);
            LOG.info("Postcondition of {} is {}{}", new Object[]{behavior.getLongName(), postcondition, code});
            targetClass.addMethod(postcondition);
        }
        catch (CannotCompileException ccx) {
            LOG.error(" *** CODE: {}", code, ccx);
            throw ccx;
        }
    }

}
