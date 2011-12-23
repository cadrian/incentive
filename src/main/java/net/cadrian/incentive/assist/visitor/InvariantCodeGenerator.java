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
import net.cadrian.incentive.assist.SyntaxException;
import net.cadrian.incentive.assist.assertion.AssertionArg;
import net.cadrian.incentive.assist.assertion.AssertionOld;
import net.cadrian.incentive.assist.assertion.AssertionResult;
import net.cadrian.incentive.assist.assertion.InvariantAssertion;
import net.cadrian.incentive.assist.ClassInstrumentor;

import javassist.CtClass;

class InvariantCodeGenerator extends AssertionCodeGenerator implements InvariantAssertion.Visitor {

    private final ClassInstrumentor classInstrumentor;
    private final Assertion assertion;
    private boolean checked = false;

    InvariantCodeGenerator(final Map<String, String> generics, final ClassInstrumentor classInstrumentor, final Assertion assertion) {
        super(generics);
        this.classInstrumentor = classInstrumentor;
        this.assertion = assertion;
        code.append("try {\n")
            .append(ClassInstrumentor.INVARIANT_FLAG_VAR)
            .append(" = true;\n");
    }

    private void check(final String localCheck) {
        code.append("if (!(")
            .append(localCheck)
            .append(")) throw new ")
            .append(ClassInstrumentor.INVARIANT_ERROR_NAME)
            .append("(\"")
            .append(classInstrumentor.getName())
            .append(": ")
            .append(assertion.toString().replace("\n", "\\n").replace("\"", "\\\""))
            .append(" is broken\");\n");
        checked = true;
    }

    @Override
    protected String getCode() {
        if (!checked) {
            return "{}";
        }
        code.append("} catch (")
            .append(ClassInstrumentor.INVARIANT_ERROR_NAME)
            .append(" x) {\nthrow x;\n} catch (Exception x) {\n")
            .append("throw new ")
            .append(ClassInstrumentor.INVARIANT_ERROR_NAME)
            .append("(\"")
            .append(classInstrumentor.getName())
            .append(": ")
            .append(assertion.toString().replace("\n", "\\n").replace("\"", "\\\""))
            .append(", \" + x.getMessage(), x);\n} finally {\n")
            .append(ClassInstrumentor.INVARIANT_FLAG_VAR)
            .append(" = false;\n}\n");
        return super.getCode();
    }

    @Override
    public void visitInvariant(final InvariantAssertion invariant){
        for (final Map.Entry<CtClass, List<Assertion>> classContract: invariant.getContract().entrySet()) {
            code.append("/*")
                .append(classContract.getKey().getName())
                .append("*/\n");
            for (final Assertion assertion: classContract.getValue()) {
                final String localFlag = local.name();
                assertion.accept(this);
                check(localFlag);
            }
        }
    }

    @Override
    public void visitArg(final AssertionArg arg){
        throw new SyntaxException("no arg allowed in invariant!");
    }

    @Override
    public void visitOld(final AssertionOld old){
        throw new SyntaxException("no old allowed in invariant!");
    }

    @Override
    public void visitResult(final AssertionResult result){
        throw new SyntaxException("no result allowed in invariant!");
    }

}
