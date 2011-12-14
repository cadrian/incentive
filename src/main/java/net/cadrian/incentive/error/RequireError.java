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
package net.cadrian.incentive.error;

/**
 * Error thrown when a precondition is not met
 *
 * @author cadrian
 */
public class RequireError extends IncentiveError {

    private static final long serialVersionUID = -7352645302882679892L;

    /**
     * @param msg
     */
    public RequireError(final String msg) {
        super(msg);
    }

    /**
     * @param msg
     * @param t
     */
    public RequireError(final String msg, final Throwable t) {
        super(msg, t);
    }

}
