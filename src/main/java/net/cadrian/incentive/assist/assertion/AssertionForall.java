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

import net.cadrian.incentive.assist.Assertion;

public class AssertionForall implements Assertion {

    public static interface Visitor extends net.cadrian.incentive.assist.Visitor {
        void visitForall(final AssertionForall forall);
    }

    public void accept(net.cadrian.incentive.assist.Visitor v) {
        ((Visitor)v).visitForall(this);
    }

    public final String type;
    public final String var;
    public final AssertionSequence value;
    public final AssertionSequence assertion;

    AssertionForall(final String type, final String var, final AssertionSequence value, final AssertionSequence assertion) {
        this.type = type;
        this.var = var;
        this.value = value;
        this.assertion = assertion;
    }

}
