package org.openl.grammar;

import java.util.Stack;

public class BracketMatcher {

    enum Brackets {
        ROUND("()"),
        CURLY("{}"),
        SQUARE("[]");

        private String brackets;

        Brackets(String brackets) {
            this.brackets = brackets;
        }

        boolean isOpen(char c) {
            return c == brackets.charAt(0);
        }

        boolean isClosed(char c) {
            return c == brackets.charAt(1);
        }

        static Brackets isBracket(char c) {
            for (int i = 0; i < values().length; i++) {

                Brackets test = values()[i];
                if (test.isClosed(c) || test.isOpen(c)) {
                    return test;
                }

            }

            return null;
        }

    }

    public static class BracketsStackObject {
        public Brackets getBracket() {
            return bracket;
        }

        public Object getId() {
            return id;
        }

        public String getErrorCode() {
            return errorCode;
        }

        Brackets bracket;
        Object id;
        String errorCode;

        public BracketsStackObject(Brackets bracket, Object id, String errorCode) {
            super();
            this.bracket = bracket;
            this.id = id;
            this.errorCode = errorCode;
        }

        public BracketsStackObject(Brackets bracket, Object id) {
            super();
            this.bracket = bracket;
            this.id = id;
        }
    }

    public static final String UNEXPECTED = "Unexpected";
    public static final String MISMATCHED = "Mismatched";
    public static final String UNMATCHED = "Unmatched";

    Stack<BracketsStackObject> stack = new Stack<>();

    public BracketsStackObject addToken(String image, Object id) {

        char c = image.charAt(0);
        Brackets b = Brackets.isBracket(c);

        if (b == null) {
            return null;
        }

        if (b.isOpen(c)) {
            stack.push(new BracketsStackObject(b, id));
            return null;
        }

        if (b.isClosed(c)) {
            if (stack.isEmpty()) {
                return new BracketsStackObject(b, id, UNEXPECTED);
            }

            BracketsStackObject bso = stack.pop();
            if (bso.bracket.isClosed(c)) {
                return null;
            }
            bso.errorCode = MISMATCHED;
            return bso;
        }

        throw new RuntimeException("Wrong bracket - cannot happen.");

    }

    public BracketsStackObject checkAtTheEnd() {
        if (stack.isEmpty()) {
            return null;
        }

        BracketsStackObject bso = stack.pop();
        bso.errorCode = UNMATCHED;
        return bso;
    }

}
