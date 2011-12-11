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
import java.util.regex.Pattern;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The javaagent main class
 * 
 * @author cadrian
 * 
 */
public class Instrumentor implements ClassFileTransformer {
	private static final Logger LOG = LoggerFactory
			.getLogger(Instrumentor.class);

	private final Map<String, byte[]> instrumentedClasses;
	private final String cacheDirectory;

	/**
	 * The javaagent main method
	 * 
	 * @param options
	 *            the options given by the javaagent framework (come from the
	 *            command line)
	 * @param ins
	 */
	public static void premain(final String options, final Instrumentation ins) {
		LOG.info("Starting Incentive...");
		ins.addTransformer(new Instrumentor(options));
		LOG.debug("Incentive started.");
	}

	private Instrumentor(final String options) {
		instrumentedClasses = new HashMap<String, byte[]>();
		if (options != null) {
			parseOptions(options);
			cacheDirectory = Option.cache.getValue();
			if (cacheDirectory != null) {
				final File classfileDir = new File(cacheDirectory);
				if (!classfileDir.exists()) {
					classfileDir.mkdirs();
				}
				LOG.info(
						"Using cache directory for instrumented class files: {}",
						classfileDir);
			}
		} else {
			cacheDirectory = null;
		}
	}

	private static void parseOptions(final String options) {
		final StringTokenizer tokenizer = new StringTokenizer(options, ",");
		while (tokenizer.hasMoreTokens()) {
			final String token = tokenizer.nextToken();
			final int equalsIndex = token.indexOf('=');
			if (equalsIndex == 0) {
				LOG.warn("Invalid option: '" + token + "'.");
			} else {
				final String name = token.substring(0, equalsIndex)
						.toLowerCase();
				if (Option.has(name)) {
					final String value;
					if (equalsIndex == -1) {
						value = null;
					} else {
						value = token.substring(equalsIndex + 1);
					}
					Option.set(name, value);
				} else {
					LOG.warn("Invalid option: '" + token + "'.");
				}
			}
		}
		LOG.debug("Found options: " + options);
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

		if (Option.limit.isSet()
				&& !Pattern.matches(Option.limit.getValue(), className)) {
			return classfileBuffer;
		}

		LOG.info("Gathering contracts for {}.", className);
		try {
			final ClassPool pool = makePool(loader, classfileBuffer, className);
			final CtClass targetClass = pool.get(className);

			// Make sure that all parents with contracts are instrumented first,
			// so that their contracts are available to this class to use.
			final List<CtClass> classHierarchy = InstrumentorUtil
					.getParents(targetClass);
			for (int i = classHierarchy.size(); i-- > 0;) {
				instrumentClass(classHierarchy.get(i), pool);
			}

			final byte[] result = instrumentClass(targetClass, pool);
			if (result == null) {
				return classfileBuffer;
			}

			if (cacheDirectory != null) {
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

	private ClassPool makePool(final ClassLoader loader,
			final byte[] classfileBuffer, final String className) {
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
		return pool;
	}

	private byte[] instrumentClass(final CtClass a_targetClass,
			final ClassPool a_pool) throws NotFoundException,
			CannotCompileException, IOException, ClassNotFoundException {
		final String targetClassName = a_targetClass.getName();
		final ClassInstrumentor classInstrumentor = new ClassInstrumentor(
				a_targetClass, a_pool);

		byte[] byteCode = instrumentedClasses.get(targetClassName);
		if (byteCode != null) {
			if (byteCode.length == a_targetClass.toBytecode().length) {
				LOG.debug("{} already instrumented.", targetClassName);
				return byteCode;
			}
			a_targetClass.defrost(); // was frozen by toByteCode()
		}

		classInstrumentor.instrument();

		if (a_targetClass.isModified()) {
			byteCode = a_targetClass.toBytecode();
			instrumentedClasses.put(targetClassName, byteCode);
			LOG.info("Instrumented {}.", targetClassName);
		} else {
			LOG.info("Class not changed {}.", targetClassName);
		}
		return byteCode;
	}

}