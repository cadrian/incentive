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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Instrumentor implements ClassFileTransformer {
	private static final Logger LOG = LoggerFactory
			.getLogger(Instrumentor.class);

	private static final String INITIALIZED_FLAG_VAR = "__incentive_initialized__";
	private static final String OLD_LOCAL_VAR = "__incentive_old__";

	private static final String INVARIANT_METHOD_NAME(final String name) {
		return String.format("__incentive_inv_%s__", name);
	}

	private static final String PRECONDITION_METHOD_NAME(final String name) {
		return String.format("__incentive_req_%s__", name);
	}

	private static final String POSTCONDITION_METHOD_NAME(final String name) {
		return String.format("__incentive_ens_%s__", name);
	}

	private Map<String, byte[]> instrumentedClasses;
	private String cacheDirectory;

	public static void premain(final String options, final Instrumentation ins) {
		ins.addTransformer(new Instrumentor(options));
	}

	public Instrumentor(final String options) {
		init(options);
	}

	private void init(final String options) {
		instrumentedClasses = new HashMap<String, byte[]>();
		if (options != null) {
			final StringTokenizer tokenizer = new StringTokenizer(options, ",");
			while (tokenizer.hasMoreTokens()) {
				final String token = tokenizer.nextToken();
				final int equalsIndex = token.indexOf('=');
				if (equalsIndex == -1 || equalsIndex == 0) {
					LOG.warn("Invalid option: '" + token + "'.");
				} else {
					final String name = token.substring(0, equalsIndex)
							.toLowerCase();
					if (Option.has(name)) {
						final String value = token.substring(equalsIndex + 1);
						Option.valueOf(name).setValue(value);
					} else {
						LOG.warn("Invalid option: '" + token + "'.");
					}
				}
			}
			LOG.debug("Found options: " + options);

			cacheDirectory = Option.cache.getValue();
			if (cacheDirectory != null) {
				final File classfileDir = new File(cacheDirectory);
				if (!classfileDir.exists()) {
					classfileDir.mkdirs();
				}
				LOG.debug(
						"Using cache directory for instrumented class files: {}",
						classfileDir);
			}
		}
	}

	@Override
	public byte[] transform(final ClassLoader loader,
			final String classNameWithSlashes,
			final Class<?> classBeingRedefined,
			final ProtectionDomain protectionDomain,
			final byte[] classfileBuffer) {
		if (classNameWithSlashes.startsWith("sunw/")
				|| classNameWithSlashes.startsWith("sun/")
				|| classNameWithSlashes.startsWith("java/")
				|| classNameWithSlashes.startsWith("javax/")
				|| classNameWithSlashes.startsWith("com/sun/")) {
			return classfileBuffer;
		}

		// classNameWithSlashes is on the format "java/lang/Object", but
		// ClassPool wants the name to be like "java.lang.Object".
		final String className = classNameWithSlashes.replace('/', '.');
		LOG.debug("Finding contracts for '" + className + "'.");
		try {
			final ClassPool pool = new ClassPool(true);
			if (loader == null) {
				pool.appendSystemPath();
			} else {
				pool.insertClassPath(new LoaderClassPath(loader));
			}

			// If this class has already been instrumented before, make sure to
			// use that code
			if (classfileBuffer != null && classfileBuffer.length > 0) {
				pool.insertClassPath(new ByteArrayClassPath(className,
						classfileBuffer));
			}
			final byte[] byteCode = instrumentedClasses.get(className);
			if (byteCode != null && byteCode.length > 0) {
				pool.insertClassPath(new ByteArrayClassPath(className, byteCode));
			}

			final CtClass targetClass = pool.get(className);
			if (targetClass.isInterface()) {
				// We're only instrumenting classes
				return classfileBuffer;
			}

			if (!InstrumentorUtil.hasDBC(targetClass)) {
				// DBC annotation absent or skip=true
				return classfileBuffer;
			}

			// Make sure that all parents with contracts are instrumented first,
			// so that their contracts are available to this class to use.
			final List<CtClass> classHierarchy = InstrumentorUtil
					.getParents(targetClass);
			classHierarchy.add(0, targetClass);
			for (int i = classHierarchy.size(); i-- > 0;) {
				instrumentClass(classHierarchy.get(i), pool);
			}

			final byte[] result = instrumentedClasses.get(className);
			if (result != null && cacheDirectory != null) {
				try {
					final File f = new File(cacheDirectory, className
							+ ".class");
					f.createNewFile();
					final OutputStream o = new FileOutputStream(f);
					o.write(result);
					o.flush();
					o.close();
					LOG.info("Wrote {} to file {}.", className, f);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			return result;
		} catch (final NotFoundException e) {
			LOG.error("Unable to load class: {}.", className, e);
			LOG.debug("ClassLoader: {}.", loader);
		} catch (final Exception e) {
			LOG.error("Unable to load class: {}.", className, e);
		}
		return classfileBuffer;
	}

	private byte[] instrumentClass(final CtClass a_targetClass,
			final ClassPool a_pool) throws NotFoundException,
			CannotCompileException, IOException {
		final String targetClassName = a_targetClass.getName();

		byte[] byteCode = instrumentedClasses.get(targetClassName);
		if (byteCode != null) {
			if (byteCode.length == a_targetClass.toBytecode().length) {
				LOG.debug("{} already instrumented.", targetClassName);
				return byteCode;
			}
			a_targetClass.defrost();
		}

		addInitializedFlag(a_targetClass);
		addInvariantMethod(a_targetClass);
		instrumentConstructors(a_targetClass);
		instrumentMethods(a_targetClass, a_pool);

		if (a_targetClass.isModified()) {
			byteCode = a_targetClass.toBytecode();
			instrumentedClasses.put(targetClassName, byteCode);
		}
		LOG.debug("Instrumented {}.", targetClassName);
		return byteCode;
	}

	private void addInitializedFlag(final CtClass a_targetClass)
			throws CannotCompileException {
		if (!InstrumentorUtil.fieldExistsInClass(a_targetClass,
				INITIALIZED_FLAG_VAR)) {
			final CtField initializedFlag = CtField.make("private boolean "
					+ INITIALIZED_FLAG_VAR + " = false;", a_targetClass);
			a_targetClass.addField(initializedFlag);
		}
	}

	private void instrumentMethods(final CtClass a_targetClass,
			final ClassPool a_pool) throws CannotCompileException,
			NotFoundException {
		final CtMethod[] targetMethods = a_targetClass.getDeclaredMethods();
		for (final CtMethod targetMethod : targetMethods) {
			instrumentMethod(a_targetClass, targetMethod, a_pool);
		}
	}

	private void instrumentMethod(final CtClass a_targetClass,
			final CtMethod a_targetMethod, final ClassPool a_pool)
			throws CannotCompileException, NotFoundException {
		final boolean isPure = InstrumentorUtil.isPure(a_targetMethod);
		addPreconditionMethod(a_targetMethod, a_pool);
		addPostconditionMethod(a_targetMethod, a_pool);
		addClassInvariantCall(a_targetMethod, a_pool, false);
		addPreconditionCall(a_targetMethod, a_pool);
		addPostconditionCall(a_targetMethod, a_pool);
		if (!isPure) {
			addClassInvariantCall(a_targetMethod, a_pool, true);
		}
	}

	private boolean addClassInvariantCall(final CtMethod targetMethod,
			final ClassPool pool, final boolean after)
			throws CannotCompileException, NotFoundException {
		final CtClass targetClass = targetMethod.getDeclaringClass();
		final String inv = INVARIANT_METHOD_NAME(targetClass.getSimpleName());
		if (!InstrumentorUtil.methodExistsInClass(targetClass, inv, "()V")
				|| InstrumentorUtil.instrumentedWith(targetMethod, inv, "()V")
				|| targetMethod.getDeclaringClass().equals(
						pool.get("java.lang.Object"))
				|| Modifier.isAbstract(targetMethod.getModifiers())
				|| Modifier.isStatic(targetMethod.getModifiers())) {
			return false;
		}
		// Only verify invariant if the instance has been fully created
		if (after) {
			targetMethod.insertAfter(String.format("if (%s) {%s();}",
					INITIALIZED_FLAG_VAR, inv));
		} else {
			targetMethod.insertBefore(String.format("if (%s) {%s();}",
					INITIALIZED_FLAG_VAR, inv));
		}
		return true;
	}

	private boolean addPostconditionCall(final CtMethod a_targetMethod,
			final ClassPool a_pool) throws CannotCompileException,
			NotFoundException {
		final String ens = POSTCONDITION_METHOD_NAME(a_targetMethod.getName());
		final CtClass targetClass = a_targetMethod.getDeclaringClass();
		final String postconditionSignature = InstrumentorUtil
				.makePostconditionSignature(a_targetMethod.getReturnType(),
						a_targetMethod.getSignature(), a_pool);
		if (!InstrumentorUtil.methodExistsInClass(targetClass, ens,
				postconditionSignature)
				|| InstrumentorUtil.instrumentedWith(a_targetMethod, ens,
						postconditionSignature)
				|| Modifier.isAbstract(a_targetMethod.getModifiers())) {
			return false;
		}
		a_targetMethod.insertAfter(String.format("%s(%s,$_,$$);", ens,
				OLD_LOCAL_VAR));
		return true;
	}

	private boolean addPreconditionCall(final CtMethod a_targetMethod,
			final ClassPool a_pool) throws CannotCompileException,
			NotFoundException {
		final String req = PRECONDITION_METHOD_NAME(a_targetMethod.getName());
		final String preconditionSignature = InstrumentorUtil
				.makePreconditionSignature(a_targetMethod.getSignature(),
						a_pool);
		final CtClass targetClass = a_targetMethod.getDeclaringClass();
		if (!InstrumentorUtil.methodExistsInClass(targetClass, req,
				preconditionSignature)
				|| InstrumentorUtil.instrumentedWith(a_targetMethod, req,
						preconditionSignature)
				|| Modifier.isAbstract(a_targetMethod.getModifiers())) {
			return false;
		}
		a_targetMethod.insertBefore(String.format("final %s = %s($$);",
				OLD_LOCAL_VAR, req));
		return true;
	}

	private void instrumentConstructors(final CtClass targetClass)
			throws CannotCompileException, NotFoundException {
		final CtConstructor[] constructors = targetClass.getConstructors();
		for (final CtConstructor constructor : constructors) {
			if (InstrumentorUtil.methodExistsInClass(constructor
					.getDeclaringClass(), "pre_" + constructor.getName(),
					InstrumentorUtil.makePreconditionSignature(constructor
							.getSignature()))
					&& !InstrumentorUtil.instrumentedWith(constructor, "pre_"
							+ constructor.getName(), InstrumentorUtil
							.makePreconditionSignature(constructor
									.getSignature()))) {
				constructor.insertBeforeBody("pre_" + constructor.getName()
						+ "($$);");
			}

			if (InstrumentorUtil.methodExistsInClass(constructor
					.getDeclaringClass(), "post_" + constructor.getName(),
					InstrumentorUtil.makePreconditionSignature(constructor
							.getSignature()))
					&& !InstrumentorUtil.instrumentedWith(constructor, "post_"
							+ constructor.getName(), InstrumentorUtil
							.makePreconditionSignature(constructor
									.getSignature()))) {
				final String setReturnValueMethodName = InstrumentorUtil
						.getSetReturnValueMethodName(constructor);
				constructor.insertAfter(setReturnValueMethodName + "(null);");
				constructor.insertAfter("post_" + constructor.getName()
						+ "($$);");
			}

			// Can't just call classInvariant from here, cuse if
			// classInvariant() calls super.classInvariant() then it's not
			// allowed from a constructor
			final CtField[] fields = targetClass.getDeclaredFields();
			for (final CtField field : fields) {
				if (field.getName().startsWith("___contract")
						&& InstrumentorUtil.methodExistsInClass(
								field.getType(), "classInvariant", "()V")) {
					constructor.insertAfter(field.getName()
							+ ".classInvariant();");
					constructor
							.insertAfter("net.sourceforge.c4j.ContractBase.classInvariantCheck(\""
									+ targetClass.getName() + "\");");
				}
			}
			if (InstrumentorUtil.fieldExistsInClass(targetClass,
					INITIALIZED_FLAG_VAR)) {
				constructor.insertAfter(INITIALIZED_FLAG_VAR + " = true;");
			}
		}
	}

	private void addInvariantMethod(final CtClass a_targetClass) {
		// TODO Auto-generated method stub

	}

	private void addPreconditionMethod(final CtBehavior a_targetBehavior,
			final ClassPool a_pool) {
		// TODO Auto-generated method stub

	}

	private void addPostconditionMethod(final CtBehavior a_targetBehavior,
			final ClassPool a_pool) {
		// TODO Auto-generated method stub

	}

}