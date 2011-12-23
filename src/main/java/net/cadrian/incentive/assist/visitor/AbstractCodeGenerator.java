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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.cadrian.incentive.assist.assertion.AssertionArg;
import net.cadrian.incentive.assist.assertion.AssertionChunk;
import net.cadrian.incentive.assist.assertion.AssertionExists;
import net.cadrian.incentive.assist.assertion.AssertionForall;
import net.cadrian.incentive.assist.assertion.AssertionOld;
import net.cadrian.incentive.assist.assertion.AssertionResult;
import net.cadrian.incentive.assist.assertion.AssertionSequence;
import net.cadrian.incentive.assist.Assertion;
import net.cadrian.incentive.assist.ClassInstrumentor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractCodeGenerator extends CodeGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCodeGenerator.class);

    protected static interface Local {
        String name();
    }

    protected static class IteratorLocal implements Local {
        private final int value;

        IteratorLocal(final int value) {
            this.value = value;
        }

        @Override
        public String name() {
            return "b" + value;
        }

        public String iterator() {
            return "i" + value;
        }
    }

    protected static class Counter {
        private int value;

        public IteratorLocal next() {
            return new IteratorLocal(value++);
        }
    }

    private static class IteratorPreparation extends CodeGenerator {
        final AbstractCodeGenerator host;
        boolean inIterator = false;
        private final Counter counter;
        final List<Local> locals;

        IteratorPreparation(final AbstractCodeGenerator host) {
            this.host = host;
            this.counter = host.counter;
            this.locals = new ArrayList<Local>();
        }

        private IteratorLocal openLoop(final Assertion value, final boolean init) {
            final IteratorLocal result = counter.next();
            code.append("boolean ")
                .append(result.name())
                .append(" = ")
                .append(init ? "true" : "false")
                .append(";\n")
                .append("net.cadrian.collection.Iterator ")
                .append(result.iterator())
                .append(" = (");
            value.accept(this);
            code.append(").iterator();\n");
            code.append("while (!")
                .append(result.iterator())
                .append(".isEmpty()");
            if (init) {
                code.append(" || ");
            }
            else {
                code.append(" && !");
            }
            code.append(result.name())
                .append(") {\n");
            return result;
        }

        private void loop(final Assertion value, final Assertion assertion, final String type, final String var, final boolean init) {
            if (!inIterator) {
                inIterator = true;
                final IteratorLocal local = openLoop(value, init);
                final Local previousLocal = host.setLocal(local);
                code.append("final ");
                final String genType = host.generics.get(type);
                if (genType == null) {
                    code.append(type);
                }
                else {
                    code.append(genType);
                }
                code.append(' ')
                    .append(var)
                    .append(" = (")
                    .append(local.iterator())
                    .append(").item();\n");
                assertion.accept(host);
                host.setLocal(previousLocal);
                code.append(local.iterator())
                    .append(".next();\n}\n");
                inIterator = false;
                locals.add(local);
            }
        }

        @Override
        public void visitExists(final AssertionExists exists){
            loop(exists.value, exists.assertion, exists.type, exists.var, false);
        }

        @Override
        public void visitForall(final AssertionForall forall){
            loop(forall.value, forall.assertion, forall.type, forall.var, true);
        }

        @Override
        public void visitChunk(final AssertionChunk chunk){
            if (inIterator) {
                chunk.accept(host);
            }
        }

        @Override
        public void visitSequence(final AssertionSequence sequence) {
            if (inIterator && sequence.parenthesized) {
                code.append('(');
            }
            super.visitSequence(sequence);
            if (inIterator && sequence.parenthesized) {
                code.append(')');
            }
        }

        @Override
        public void visitArg(final AssertionArg arg){
            if (inIterator) {
                arg.accept(host);
            }
        }

        @Override
        public void visitOld(final AssertionOld old){
            if (inIterator) {
                old.accept(host);
            }
        }

        @Override
        public void visitResult(final AssertionResult result){
            if (inIterator) {
                result.accept(host);
            }
        }
    }

    protected final Map<String, String> generics;
    protected final Counter counter;
    protected Local local;

    protected AbstractCodeGenerator(final Map<String, String> generics) {
        this.generics = generics;
        this.counter = createCounter();
        this.local = firstLocal();
        code.append("{\n");
    }

    protected abstract Local firstLocal();

    protected Counter createCounter() {
        return new Counter();
    }

    Local setLocal(final Local local) {
        final Local result = this.local;
        this.local = local;
        return result;
    }

    protected void appendCode(final CodeGenerator generator, final Assertion assertion) {
        final StringBuilder oldCode = code;
        code = generator.code;
        assertion.accept(generator);
        oldCode.append(generator.getCode());
        code = oldCode;
    }

    protected String getCode() {
        return code.append("}\n").toString();
    }

    @Override
    public void visitChunk(final AssertionChunk chunk){
        code.append(chunk.chunk);
    }

    @Override
    public void visitExists(final AssertionExists exists){
        // nothing
    }

    @Override
    public void visitForall(final AssertionForall forall){
        // nothing
    }

    @Override
    public void visitSequence(final AssertionSequence sequence){
        if (sequence.parenthesized) {
            code.append('(');
            super.visitSequence(sequence);
            code.append(')');
        }
        else {
            final Local local = this.local;
            final IteratorPreparation preparation = new IteratorPreparation(this);
            appendCode(preparation, sequence);
            code.append(local.name())
                .append(" = (");
            final List<Local> locals = preparation.locals;
            if (locals.isEmpty()) {
                super.visitSequence(sequence);
            }
            else {
                boolean more = false;
                for (final Local prepLocal: locals) {
                    if (more) code.append("&&");
                    code.append(prepLocal.name());
                    more = true;
                }
            }
            code.append(");\n");
        }
    }

}
