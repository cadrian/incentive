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

import java.util.Map;

import net.cadrian.incentive.assist.SyntaxException;
import net.cadrian.incentive.assist.Visitor;
import net.cadrian.incentive.assist.assertion.AssertionArg;
import net.cadrian.incentive.assist.assertion.AssertionChunk;
import net.cadrian.incentive.assist.assertion.AssertionExists;
import net.cadrian.incentive.assist.assertion.AssertionForall;
import net.cadrian.incentive.assist.assertion.AssertionOld;
import net.cadrian.incentive.assist.assertion.AssertionResult;
import net.cadrian.incentive.assist.assertion.AssertionSequence;
import net.cadrian.incentive.assist.Assertion;
import net.cadrian.incentive.assist.BehaviorInstrumentor;
import net.cadrian.incentive.assist.ClassInstrumentor;

public abstract class CodeGenerator implements AssertionArg.Visitor,
                                               AssertionChunk.Visitor,
                                               AssertionExists.Visitor,
                                               AssertionForall.Visitor,
                                               AssertionOld.Visitor,
                                               AssertionResult.Visitor,
                                               AssertionSequence.Visitor,
                                               Visitor {

    public static String invariant(final Map<String, String> generics, final ClassInstrumentor classInstrumentor, final Assertion assertion) {
        return accept(new InvariantCodeGenerator(generics, classInstrumentor, assertion), assertion);
    }

    public static String ensureOld(final Map<String, String> generics, final BehaviorInstrumentor behaviorInstrumentor, final Assertion assertion) {
        return accept(new EnsureOldCodeGenerator(generics, behaviorInstrumentor, assertion), assertion);
    }

    public static String require(final Map<String, String> generics, final BehaviorInstrumentor behaviorInstrumentor, final Assertion assertion) {
        return accept(new RequireCodeGenerator(generics, behaviorInstrumentor, assertion), assertion);
    }

    public static String ensure(final Map<String, String> generics, final BehaviorInstrumentor behaviorInstrumentor, final Assertion assertion) {
        return accept(new EnsureCodeGenerator(generics, behaviorInstrumentor, assertion), assertion);
    }

    protected static String accept(final CodeGenerator generator, final Assertion assertion) {
        assertion.accept(generator);
        return generator.getCode();
    }

    protected StringBuilder code;

    protected CodeGenerator() {
        this.code = new StringBuilder(1024);
    }

    protected void appendCode(final CodeGenerator generator, final Assertion assertion) {
        final StringBuilder oldCode = code;
        code = generator.code;
        assertion.accept(generator);
        oldCode.append(generator.getCode());
        code = oldCode;
    }

    protected String getCode() {
        return code.toString();
    }

    @Override
    public void visitSequence(final AssertionSequence sequence){
        for (final Assertion assertion: sequence.getAssertions()) {
            assertion.accept(this);
        }
    }

}
