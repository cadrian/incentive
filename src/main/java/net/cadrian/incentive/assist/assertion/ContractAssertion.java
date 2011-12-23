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
package net.cadrian.incentive.assist.assertion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.cadrian.incentive.assist.Assertion;

import javassist.CtClass;

abstract class ContractAssertion implements Assertion {
    private final Map<CtClass, List<Assertion>> contract;
    private final int[] oldIndex;

    public ContractAssertion() {
        this.contract = new LinkedHashMap<CtClass, List<Assertion>>();
        oldIndex = new int[]{0};
    }

    public void add(final CtClass targetClass, final String assertion) {
        final AssertionParser parser = new AssertionParser(assertion, oldIndex);
        List<Assertion> classContract = contract.get(targetClass);
        if (classContract == null) {
            classContract = new ArrayList<Assertion>();
            contract.put(targetClass, classContract);
        }
        classContract.add(parser.parse());
    }

    public Map<CtClass, List<Assertion>> getContract() {
        return contract;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder("{\n");
        for (final Map.Entry<CtClass, List<Assertion>> classContract: contract.entrySet()) {
            result.append(" - ").append(classContract.getKey().getName()).append(" => [");
            for (final Assertion assertion: classContract.getValue()) {
                result.append('{').append(assertion).append('}');
            }
            result.append("]\n");
        }
        result.append('}');
        return result.toString();
    }

}
