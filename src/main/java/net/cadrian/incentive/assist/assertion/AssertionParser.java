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

public class AssertionParser {

    public static class SyntaxException extends RuntimeException {
        SyntaxException(final String src, final int pos) {
            super("{" + src + "} at " + pos);
        }
    }

    private static enum Keyword {
        result {
            @Override
            void parse(final AssertionParser parser) {
                parser.parseResult();
            }
        }, old {
            @Override
            void parse(final AssertionParser parser) {
                parser.parseOld();
            }
        }, forall {
            @Override
            void parse(final AssertionParser parser) {
                parser.parseForall();
            }
        }, exists {
            @Override
            void parse(final AssertionParser parser) {
                parser.parseExists();
            }
        };

        abstract void parse(final AssertionParser parser);
    }

    private final char[] src;
    private int pos;

    public AssertionParser(final String assertion) {
        src = assertion.toCharArray();
        pos = 0;
    }

    private AssertionSequence lastAssertion;

    public Assertion parse() {
        parseAssertion();
        return lastAssertion;
    }

    private void parseAssertion() {
        skipBlanks();
        final StringBuilder buffer = new StringBuilder();
        while (pos < src.length && src[pos] != '{') {
            buffer.append(src[pos]);
        }
        if (buffer.length() > 0) {
            lastAssertion.add(new AssertionChunk(buffer.toString()));
        }
        if (pos < src.length && src[pos] == '{') {
            parseOperator();
        }
        if (pos < src.length) {
            parseAssertion();
        }
    }

    private void parseOperator() {
        skip('{');
        skipBlanks();
        final String operator = parseWord();
        if (operator == null) {
            throw new SyntaxException(new String(src), pos);
        }
        final Keyword k = Keyword.valueOf(operator);
        if (k == null) {
            throw new SyntaxException(new String(src), pos);
        }
        k.parse(this);
        skip('}');
    }

    private void parseResult() {
        lastAssertion.add(new AssertionResult());
    }

    private static interface NestAssertion {
        Assertion nest(final AssertionSequence nested);
    }

    private void parseNestedAssertion(final NestAssertion nester) {
        skipBlanks();
        final AssertionSequence oldSequence = lastAssertion;
        lastAssertion = new AssertionSequence();
        parseAssertion();
        oldSequence.add(nester.nest(lastAssertion));
        lastAssertion = oldSequence;
    }

    private static final NestAssertion OLD_NESTER = new NestAssertion() {
            @Override
            public Assertion nest(final AssertionSequence nested) {
                return new AssertionOld(nested);
            }
        };

    private void parseOld() {
        parseNestedAssertion(OLD_NESTER);
    }

    private static final NestAssertion FORALL_NESTER(final String type, final String var, final AssertionSequence value) {
        return new NestAssertion() {
            @Override
            public Assertion nest(final AssertionSequence nested) {
                return new AssertionForall(type, var, value, nested);
            }
        };
    }

    private static final NestAssertion EXISTS_NESTER(final String type, final String var, final AssertionSequence value) {
        return new NestAssertion() {
            @Override
            public Assertion nest(final AssertionSequence nested) {
                return new AssertionExists(type, var, value, nested);
            }
        };
    }

    private static interface NestAssertionFactory {
        NestAssertion createNester(final String type, final String var, final AssertionSequence value);
    }

    private void parseTypedOperator(final NestAssertionFactory nestFactory) {
        skip('(');

        skipBlanks();
        final String type = parseType();
        if (type == null) {
            throw new SyntaxException(new String(src), pos);
        }

        skipBlanks();
        final String var = parseWord();
        if (var == null) {
            throw new SyntaxException(new String(src), pos);
        }

        skip(':');

        skipBlanks();
        final AssertionSequence oldSequence = lastAssertion;
        lastAssertion = new AssertionSequence();
        parseAssertion();
        final AssertionSequence value = lastAssertion;
        lastAssertion = oldSequence;

        skip(')');

        parseNestedAssertion(nestFactory.createNester(type, var, value));
    }

    private static final NestAssertionFactory FORALL_NESTER_FACTORY = new NestAssertionFactory() {
            @Override
            public NestAssertion createNester(final String type, final String var, final AssertionSequence value) {
                return FORALL_NESTER(type, var, value);
            }
        };

    private static final NestAssertionFactory EXISTS_NESTER_FACTORY = new NestAssertionFactory() {
            @Override
            public NestAssertion createNester(final String type, final String var, final AssertionSequence value) {
                return EXISTS_NESTER(type, var, value);
            }
        };

    private void parseForall() {
        parseTypedOperator(FORALL_NESTER_FACTORY);
    }

    private void parseExists() {
        parseTypedOperator(EXISTS_NESTER_FACTORY);
    }

    private String parseWord() {
        final int pos0 = pos;
        if (pos < src.length && Character.isJavaIdentifierStart(src[pos])) {
            pos++;
            while (pos < src.length && Character.isJavaIdentifierPart(src[pos])) {
                pos++;
            }
        }
        if (pos == pos0) {
            return null;
        }
        return new String(src, pos0, pos-pos0);
    }

    private String parseType() {
        StringBuilder result = new StringBuilder();
        boolean done = false;
        while (!done) {
            final String word = parseWord();
            if (word == null) {
                done = true;
            }
            else {
                result.append(word);
                if (pos < src.length && src[pos] == '.') {
                    result.append('.');
                    pos++;
                }
                else {
                    done = true;
                }
            }
        }
        if (result.length() == 0) {
            return null;
        }
        return result.toString();
    }

    private void skipBlanks() {
        while (pos < src.length && Character.isWhitespace(src[pos])) {
            pos++;
        }
    }

    private void skip(final char c) {
        skipBlanks();
        if (pos >= src.length || src[pos] != c) {
            throw new SyntaxException(new String(src), pos);
        }
    }

}
