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

import net.cadrian.incentive.assist.Visitor;
import net.cadrian.incentive.assist.assertion.AssertionArg;
import net.cadrian.incentive.assist.assertion.AssertionChunk;
import net.cadrian.incentive.assist.assertion.AssertionExists;
import net.cadrian.incentive.assist.assertion.AssertionForall;
import net.cadrian.incentive.assist.assertion.AssertionOld;
import net.cadrian.incentive.assist.assertion.AssertionResult;
import net.cadrian.incentive.assist.assertion.AssertionSequence;
import net.cadrian.incentive.assist.Assertion;
import net.cadrian.incentive.assist.ClassInstrumentor;

public abstract class CodeGenerator implements
                                   AssertionArg.Visitor,
                                   AssertionChunk.Visitor,
                                   AssertionExists.Visitor,
                                   AssertionForall.Visitor,
                                   AssertionOld.Visitor,
                                   AssertionResult.Visitor,
                                   AssertionSequence.Visitor,
                                   Visitor {

    public static String invariant(final ClassInstrumentor classInstrumentor, final Assertion assertion) {
        return accept(new InvariantCodeGenerator(classInstrumentor), assertion);
    }

    private static String accept(final CodeGenerator generator, final Assertion assertion) {
        try {
            assertion.accept(generator);
            return generator.getCode();
        } catch (RuntimeException rx) {
            rx.printStackTrace();
            throw rx;
        }
    }

    protected final StringBuilder code;

    protected CodeGenerator() {
        this.code = new StringBuilder();
    }

    protected String getCode() {
        return code.toString();
    }

    @Override
    public void visitSequence(final AssertionSequence sequence) {
        for (final Assertion assertion: sequence.getAssertions()) {
            assertion.accept(this);
        }
    }

}
