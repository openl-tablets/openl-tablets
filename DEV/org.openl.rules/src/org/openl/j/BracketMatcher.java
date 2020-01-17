package org.openl.j;

import java.util.Stack;

class BracketMatcher {

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

        public Object getId() {
            return id;
        }

        ErrorType getErrorCode() {
            return errorCode;
        }

        Brackets bracket;
        Object id;
        ErrorType errorCode;

        BracketsStackObject(Brackets bracket, Object id, ErrorType errorCode) {
            super();
            this.bracket = bracket;
            this.id = id;
            this.errorCode = errorCode;
        }

        BracketsStackObject(Brackets bracket, Object id) {
            super();
            this.bracket = bracket;
            this.id = id;
        }
    }

    private Stack<BracketsStackObject> stack = new Stack<>();

    BracketsStackObject addToken(String image, Object id) {

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
                return new BracketsStackObject(b, id, ErrorType.UNEXPECTED);
            }

            BracketsStackObject bso = stack.pop();
            if (bso.bracket.isClosed(c)) {
                return null;
            }
            bso.errorCode = ErrorType.MISMATCHED;
            return bso;
        }

        throw new IllegalStateException("Wrong bracket - cannot happen.");

    }

    BracketsStackObject checkAtTheEnd() {
        if (stack.isEmpty()) {
            return null;
        }

        BracketsStackObject bso = stack.pop();
        bso.errorCode = ErrorType.UNMATCHED;
        return bso;
    }

    public enum ErrorType {
        UNEXPECTED,
        MISMATCHED,
        UNMATCHED
    }
}
