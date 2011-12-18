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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.compiler.ast.ASTree;
import javassist.compiler.CompileError;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.Lex;
import javassist.compiler.Parser;
import javassist.compiler.SymbolTable;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Bytecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TransformCodecs {
    private static final Logger LOG = LoggerFactory.getLogger(TransformCodecs.class);

    private TransformCodecs() {
    }

    static final Pattern RESULT_REGEXP = Pattern.compile("\\{result\\}");
    static final Pattern OLD_REGEXP = Pattern.compile("\\{old\\s+([^}]+)\\}");
    static final Pattern ARG_REGEXP = Pattern.compile("\\{arg\\s+([1-9][0-9]*)\\}");

    // The {result} -- in postconditions only
    static final TransformCodec POSTCONDITION_RESULT_CODEC = new TransformCodec() {
        @Override
        public String decode(final String assertion, final CtClass targetClass, final ClassPool pool) {
            return RESULT_REGEXP.matcher(assertion).replaceAll("\\$2");
        }
    };

    private static SymbolTable gatherParamSymbols(final ClassPool pool, final CtClass targetClass, final CtBehavior behavior) throws NotFoundException, CompileError {
        final SymbolTable result = new SymbolTable();
        final CtClass[] params = behavior.getParameterTypes();
        final MethodInfo info = behavior.getMethodInfo2();
        final Bytecode bc = new Bytecode(info.getConstPool(), 0, 0);
        final JvstCodeGen gen = new JvstCodeGen(bc, targetClass, pool);
        gen.recordParams(params, false, "$", "$args", "$$", true, 0, targetClass.getName(), result);
        return result;
    }

    private static class OldClassTransformCodec implements TransformCodec {
        int index;
        private final CtClass preconditionClass;
        private final SymbolTable symbolTable;

        OldClassTransformCodec(final ClassPool pool, final CtClass targetClass, final CtClass a_preconditionClass, final int a_index, final CtBehavior behavior)
            throws NotFoundException, CompileError {
            this.index = a_index;
            this.preconditionClass = a_preconditionClass;
            this.symbolTable = gatherParamSymbols(pool, targetClass, behavior);
        }

        @SuppressWarnings("boxing")
        @Override
        public String decode(final String assertion, final CtClass targetClass, final ClassPool pool)
            throws CannotCompileException, CompileError {
            final StringBuffer result = new StringBuffer(String.format("/*OldClassTransformCodec:%d*/", index));
            boolean found = false;
            final Matcher matcher = OLD_REGEXP.matcher(assertion);
            while (matcher.find()) {
                final String src = matcher.group(1);
                final CtClass type = expressionType(src, targetClass, pool, symbolTable);
                LOG.debug("Type of old{} is {}", index, type.getName());
                final CtField field = new CtField(type, String.format("old%d", index++), preconditionClass);
                field.setModifiers(Modifier.PUBLIC);
                preconditionClass.addField(field);
                found = true;
            }

            return found ? result.toString() : null;
        }
    }

    // The {old} class -- in preconditions only
    static TransformCodec PRECONDITION_OLD_CLASS_CODEC(final ClassPool pool, final CtClass targetClass, final CtBehavior targetBehavior, final CtClass preconditionClass, final TransformCodec previous)
        throws NotFoundException, CompileError {
        return new OldClassTransformCodec(pool, targetClass, preconditionClass, previous == null ? 0 : ((OldClassTransformCodec) previous).index, targetBehavior);
    }

    private static class OldValuesTransformCodec implements TransformCodec {
        int index;

        OldValuesTransformCodec(final int a_index) {
            this.index = a_index;
        }

        @SuppressWarnings("boxing")
        @Override
        public final String decode(final String assertion, final CtClass targetClass, final ClassPool pool) {
            final StringBuffer result = new StringBuffer(String.format("/*OldValuesTransformCodec:%d*/", index));
            boolean found = false;
            final Matcher matcher = OLD_REGEXP.matcher(assertion);
            while (matcher.find()) {
                final String src = matcher.group(1);
                result.append(String.format("result.old%d = (%s);\n", index++, src));
                found = true;
            }

            return found ? result.toString() : null;
        }
    }

    // The {old} values -- in preconditions only
    static TransformCodec PRECONDITION_OLD_VALUES_CODEC(final TransformCodec previous) {
        return new OldValuesTransformCodec(previous == null ? 0 : ((OldValuesTransformCodec) previous).index);
    }

    private static class PostconditionOldValuesTransformCodec implements TransformCodec {
        int index;

        PostconditionOldValuesTransformCodec(final int a_index) {
            this.index = a_index;
        }

        @SuppressWarnings("boxing")
        @Override
        public final String decode(final String assertion, final CtClass targetClass, final ClassPool pool) {
            final StringBuffer result = new StringBuffer(String.format("/*PostconditionOldValuesTransformCodec:%d*/", index));
            final Matcher matcher = OLD_REGEXP.matcher(assertion);
            while (matcher.find()) {
                matcher.appendReplacement(result, String.format("((\\$1).old%d)", index++));
            }
            matcher.appendTail(result);

            return result.toString();
        }
    }

    // The {old} values -- in postconditions only
    static TransformCodec POSTCONDITION_OLD_VALUES_CODEC(final TransformCodec previous) {
        return new PostconditionOldValuesTransformCodec(previous == null ? 0 : ((PostconditionOldValuesTransformCodec) previous).index);
    }

    static final TransformCodec PRECONDITION_ARGUMENTS_CODEC = new TransformCodec() {
        @SuppressWarnings("boxing")
        @Override
        public String decode(final String assertion, final CtClass targetClass, final ClassPool pool) {
            final StringBuffer result = new StringBuffer();
            final Matcher matcher = ARG_REGEXP.matcher(assertion);
            while (matcher.find()) {
                final int index = Integer.parseInt(matcher.group(1));
                matcher.appendReplacement(result, String.format("\\$%d", index));
            }
            matcher.appendTail(result);
            return result.toString();
        }
    };

    static final TransformCodec POSTCONDITION_ARGUMENTS_CODEC = new TransformCodec() {
        @SuppressWarnings("boxing")
        @Override
        public String decode(final String assertion, final CtClass targetClass, final ClassPool pool) {
            final StringBuffer result = new StringBuffer();
            final Matcher matcher = ARG_REGEXP.matcher(assertion);
            while (matcher.find()) {
                final int index = Integer.parseInt(matcher.group(1));
                // the first two indexes are the "old" array and the result
                matcher.appendReplacement(result, String.format("\\$%d", index + 2));
            }
            matcher.appendTail(result);
            return result.toString();
        }
    };

    static CtClass expressionType(final String src, final CtClass targetClass, final ClassPool pool, final SymbolTable behaviorSymbolTable)
        throws CompileError {
        final Parser parser = new Parser(new Lex(src));
        final SymbolTable stb = new SymbolTable(behaviorSymbolTable);
        final ASTree tree = parser.parseExpression(stb);
        return new StmtTypeVisitor(targetClass, pool).getType(tree);
    }

}
