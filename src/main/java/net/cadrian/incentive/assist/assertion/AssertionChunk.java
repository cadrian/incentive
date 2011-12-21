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

public class AssertionChunk implements Assertion {

    public static interface Visitor extends net.cadrian.incentive.assist.Visitor {
        void visitChunk(final AssertionChunk chunk);
    }

    public void accept(net.cadrian.incentive.assist.Visitor v) {
        ((Visitor)v).visitChunk(this);
    }

    AssertionChunk(final String chunk) {
    }

}
