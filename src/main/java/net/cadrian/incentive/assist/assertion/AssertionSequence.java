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
import java.util.List;

import net.cadrian.incentive.assist.Assertion;


public class AssertionSequence implements Assertion {

    public static interface Visitor extends net.cadrian.incentive.assist.Visitor {
        void visitSequence(final AssertionSequence sequence);
    }

    public void accept(final net.cadrian.incentive.assist.Visitor v) {
        ((Visitor)v).visitSequence(this);
    }

    private final List<Assertion> assertions;

    AssertionSequence() {
        assertions = new ArrayList<Assertion>();
    }

    void add(final Assertion assertion) {
        assertions.add(assertion);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (final Assertion assertion: assertions) {
            result.append(assertion);
        }
        return result.toString();
    }

}
