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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import javassist.bytecode.Descriptor;
import javassist.bytecode.SignatureAttribute;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.compiler.CompileError;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import javassist.expr.Instanceof;
import javassist.expr.MethodCall;
import javassist.expr.NewArray;
import javassist.expr.NewExpr;
import javassist.NotFoundException;

import net.cadrian.incentive.DBC;
import net.cadrian.incentive.Pure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InstrumentorUtil {
    private static final Logger LOG = LoggerFactory.getLogger(InstrumentorUtil.class);

    private InstrumentorUtil() {
        // no instances
    }

    static boolean instrumentedWith(final CtBehavior method,
                                           final String methodName, final String signature)
        throws CannotCompileException {
        final boolean[] tmp = { false };
        method.instrument(new ExprEditor() {
                @Override
                public void edit(final MethodCall mcall) {
                    if (mcall.getMethodName().equals(methodName)
                        && mcall.getSignature().equals(signature)) {
                        tmp[0] = true;
                    }
                }
            });
        return tmp[0];
    }

    static boolean methodAnnotatedWithPure(final CtMethod method)
        throws ClassNotFoundException {
        for (final Object annotation : method.getAnnotations()) {
            if (annotation instanceof Pure) {
                return true;
            }
        }
        return false;
    }

    static boolean isPure(final CtMethod method)
        throws CannotCompileException, NotFoundException,
               ClassNotFoundException {
        if (methodAnnotatedWithPure(method)) {
            return true;
        }

        final List<CtClass> classHierarchy = getParents(method.getDeclaringClass());
        classHierarchy.add(0, method.getDeclaringClass());
        for (final CtClass c : classHierarchy) {
            final List<CtClass> interfaces = new ArrayList<CtClass>();
            Collections.addAll(interfaces, c.getInterfaces());
            for (int i = 0; i < interfaces.size(); i++) {
                final CtClass interfaze = interfaces.get(i);
                CtMethod meth = null;
                try {
                    meth = interfaze.getMethod(method.getName(),
                                               method.getSignature());
                    if (meth != null && methodAnnotatedWithPure(meth)) {
                        return true;
                    }
                } catch (final NotFoundException e) {
                    // ignored, meth is null
                }
                Collections.addAll(interfaces, interfaze.getInterfaces());
            }
        }

        final boolean[] tmp = { true };
        method.instrument(new ExprEditor() {
                @Override
                public void edit(final Cast c) {
                    // Should be ok
                }

                @Override
                public void edit(final ConstructorCall c) {
                    tmp[0] = false;
                }

                @Override
                public void edit(final FieldAccess f) {
                    if (!f.isReader()) {
                        tmp[0] = false;
                    }
                }

                @Override
                public void edit(final Handler h) {
                    tmp[0] = false;
                }

                @Override
                public void edit(final Instanceof i) {
                    // Should be ok
                }

                @Override
                public void edit(final MethodCall m) {
                    tmp[0] = false;
                }

                @Override
                public void edit(final NewArray a) {
                    tmp[0] = false;
                }

                @Override
                public void edit(final NewExpr e) {
                    tmp[0] = false;
                }
            });
        return tmp[0];
    }

    private static void addParents(final CtClass targetClass,
                                   final Set<CtClass> parents) throws NotFoundException {
        final CtClass parent = targetClass.getSuperclass();
        if (parent != null && !parents.contains(parent)) {
            parents.add(parent);
            addParents(parent, parents);
        }
        for (final CtClass itf : targetClass.getInterfaces()) {
            if (!parents.contains(itf)) {
                parents.add(itf);
                addParents(itf, parents);
            }
        }
    }

    static List<CtClass> getParents(final CtClass targetClass) throws NotFoundException {
        final Set<CtClass> parents = new LinkedHashSet<CtClass>();
        addParents(targetClass, parents);
        return new ArrayList<CtClass>(parents);
    }

    static boolean hasDBC(final CtClass a_targetClass) {
        try {
            final DBC dbc = (DBC) a_targetClass.getAnnotation(DBC.class);
            return dbc != null && !dbc.skip();
        } catch (final ClassNotFoundException cnfx) {
            LOG.warn("class not found???", cnfx);
        }
        return false;
    }


    private static final String JVM_TYPE = "\\[*(?:[BCDFIJSZ]|L[^;]+;)";

    private static final Pattern SIGNATURE_REGEXP = Pattern.compile("<([^>]+)>");
    private static final Pattern PARAMETER_REGEXP = Pattern.compile("([^:]+)(?:[:](" + JVM_TYPE + ")(?:[:](" + JVM_TYPE + "))*)?");
    private static final Pattern TYPE_REGEXP = Pattern.compile("(" + JVM_TYPE + ")");

    static Map<String, String> getGenericTypes(final CtClass a_targetClass) {
        final Map<String, String> result = new LinkedHashMap<String, String>();

        final SignatureAttribute signatureAttribute = (SignatureAttribute)a_targetClass.getClassFile2().getAttribute(SignatureAttribute.tag);
        if (signatureAttribute != null) {
            final String signature = signatureAttribute.getSignature();
            LOG.debug("**** Signature of " + a_targetClass.getName() + ": " + signature);
            final Matcher signatureMatcher = SIGNATURE_REGEXP.matcher(signature);
            if (signatureMatcher.lookingAt()) {
                final Matcher parameterMatcher = PARAMETER_REGEXP.matcher(signatureMatcher.group(1));
                int i = 0;
                while (parameterMatcher.find()) {
                    final String matcherName = parameterMatcher.group(1);
                    final String matcherSuperclass = parameterMatcher.group(2);
                    final String matcherSuperinterfaces = parameterMatcher.group(3);
                    if (matcherSuperclass != null) {
                        if (matcherSuperinterfaces != null) {
                            LOG.warn("{}: ignoring superinterfaces for generic parameter <{}>", a_targetClass.getName(), matcherName);
                        }
                        result.put(matcherName, Descriptor.toClassName(matcherSuperclass));
                    }
                    else if (matcherSuperinterfaces != null) {
                        final Matcher typeMatcher = TYPE_REGEXP.matcher(matcherSuperinterfaces);
                        if (typeMatcher.find()) {
                            result.put(matcherName, Descriptor.toClassName(typeMatcher.group(1)));
                            if (typeMatcher.find()) {
                                LOG.warn("{}: ignoring extra superinterfaces for generic parameter <{}>", a_targetClass.getName(), matcherName);
                            }
                        }
                    }
                    else {
                        result.put(matcherName, "java.lang.Object");
                    }
                    i++;
                }
            }
            LOG.debug("**** Signature of " + a_targetClass.getName() + ": " + result);
        }

        return result;
    }

    static String voidify(final String descriptor) {
        final int index = descriptor.indexOf(')');
        if (index < 0) {
            return descriptor;
        }
        return String.format("%sV;", descriptor.substring(0, index + 1));
    }
}
