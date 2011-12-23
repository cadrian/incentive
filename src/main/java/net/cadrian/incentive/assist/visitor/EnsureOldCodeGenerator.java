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
package net.cadrian.incentive.assist.visitor;

import java.util.List;
import java.util.Map;

import net.cadrian.incentive.assist.Assertion;
import net.cadrian.incentive.assist.assertion.AssertionArg;
import net.cadrian.incentive.assist.assertion.AssertionChunk;
import net.cadrian.incentive.assist.assertion.AssertionOld;
import net.cadrian.incentive.assist.assertion.AssertionResult;
import net.cadrian.incentive.assist.assertion.AssertionSequence;
import net.cadrian.incentive.assist.assertion.EnsureAssertion;
import net.cadrian.incentive.assist.BehaviorInstrumentor;

import javassist.bytecode.Bytecode;
import javassist.bytecode.MethodInfo;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.compiler.ast.ASTree;
import javassist.compiler.ast.Declarator;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.cadrian.incentive.assist.assertion.AssertionForall;
import net.cadrian.incentive.assist.assertion.AssertionExists;

class EnsureOldCodeGenerator extends AbstractCodeGenerator implements EnsureAssertion.Visitor {
    private static final Logger LOG = LoggerFactory.getLogger(EnsureOldCodeGenerator.class);

    private final CtClass oldClass;
    private final SymbolTable symbolTable;
    private final BehaviorInstrumentor behaviorInstrumentor;
    private final Assertion assertion;
    private boolean inOld;
    private boolean hasFields;

    protected static class OldLocal implements Local {
        private final AssertionOld old;
        OldLocal(final AssertionOld old) {
            this.old = old;
        }
        @Override
        public String name() {
            return "result.old" + old.index;
        }
    }

    private static SymbolTable gatherParamSymbols(final ClassPool pool, final CtClass targetClass, final CtBehavior behavior) {
        try {
            final SymbolTable result = new SymbolTable();
            final CtClass[] params = behavior.getParameterTypes();
            final MethodInfo info = behavior.getMethodInfo2();
            final Bytecode bc = new Bytecode(info.getConstPool(), 0, 0);
            final JvstCodeGen gen = new JvstCodeGen(bc, targetClass, pool);
            gen.recordParams(params, false, "$", "$args", "$$", true, 0, targetClass.getName(), result);
            return result;
        } catch (NotFoundException nfx) {
            LOG.error("old", nfx);
            throw new RuntimeException(nfx);
        } catch (CompileError ce) {
            LOG.error("old", ce);
            throw new RuntimeException(ce);
        }
    }

    private static CtClass expressionType(final String src, final CtClass targetClass, final ClassPool pool, final SymbolTable behaviorSymbolTable) {
        try {
            final Parser parser = new Parser(new Lex(src));
            final SymbolTable stb = new SymbolTable(behaviorSymbolTable);
            final ASTree tree = parser.parseExpression(stb);
            return new StmtTypeVisitor(targetClass, pool).getType(tree);
        } catch (CompileError ce) {
            LOG.error(src, ce);
            throw new RuntimeException(ce);
        }
    }

    protected class CounterST extends Counter {
        SymbolTable symbolTable;

        @Override
        public IteratorLocal next() {
            final IteratorLocal result = super.next();
            symbolTable.append(result.name(), new Declarator(Declarator.BOOLEAN, 0));
            return result;
        }
    }

    EnsureOldCodeGenerator(final Map<String, String> generics, final BehaviorInstrumentor behaviorInstrumentor, final Assertion assertion) {
        super(generics);
        this.behaviorInstrumentor = behaviorInstrumentor;
        this.assertion = assertion;
        this.oldClass = behaviorInstrumentor.oldValuesClass;
        this.symbolTable = gatherParamSymbols(behaviorInstrumentor.pool, behaviorInstrumentor.targetClass, behaviorInstrumentor.behavior);
        ((CounterST)counter).symbolTable = symbolTable;
    }


    protected String getCode() {
        if (!hasFields) {
            return "";
        }
        return super.getCode();
    }


    @Override
    protected Local firstLocal() {
        return null;
    }

    @Override
    protected Counter createCounter() {
        return new CounterST();
    }

    @Override
    public void visitEnsure(final EnsureAssertion ensure){
        for (final Map.Entry<CtClass, List<Assertion>> classContract: ensure.getContract().entrySet()) {
            code.append("/*")
                .append(classContract.getKey().getName())
                .append("*/\n");
            for (final Assertion assertion: classContract.getValue()) {
                assertion.accept(this);
            }
        }
    }

    @Override
    public void visitOld(final AssertionOld old){
        final boolean oldInOld = inOld;
        inOld = true;

        final Local oldLocal = local;
        local = null;
        final StringBuilder mainCode = code;
        code = new StringBuilder();
        old.old.accept(this);
        final String expr = code.toString();
        code = mainCode;

        local = new OldLocal(old);

        try {
            final CtClass type = expressionType(expr, behaviorInstrumentor.targetClass, behaviorInstrumentor.pool, symbolTable);
            final CtField field = new CtField(type, "old" + old.index, oldClass);
            field.setModifiers(Modifier.PUBLIC);
            oldClass.addField(field);
            LOG.info("Added field: {}", field);
        } catch (CannotCompileException ccx) {
            LOG.error("old", ccx);
            throw new RuntimeException(ccx);
        }

        old.old.accept(this);
        local = oldLocal;
        inOld = oldInOld;

        hasFields = true;
    }

    @Override
    public void visitArg(final AssertionArg arg){
        if (inOld) {
            code.append('$').append(arg.index);
        }
    }

    @Override
    public void visitChunk(final AssertionChunk chunk){
        if (inOld) {
            super.visitChunk(chunk);
        }
    }

    @Override
    public void visitResult(final AssertionResult result){
        if (inOld) {
            throw new RuntimeException("no {result} allowed in old expressions!");
        }
    }

    @Override
    public void visitExists(final AssertionExists exists){
        if (inOld) {
            throw new RuntimeException("no {exists} allowed in old expressions!");
        }
        super.visitExists(exists);
    }

    @Override
    public void visitForall(final AssertionForall forall){
        if (inOld) {
            throw new RuntimeException("no {forall} allowed in old expressions!");
        }
        super.visitForall(forall);
    }

    @Override
    public void visitSequence(final AssertionSequence sequence){
        if (local != null) {
            super.visitSequence(sequence);
        }
        else {
            if (inOld && sequence.parenthesized) {
                code.append('(');
            }
            for (final Assertion assertion: sequence.getAssertions()) {
                assertion.accept(this);
            }
            if (inOld && sequence.parenthesized) {
                code.append(')');
            }
        }
    }

}
