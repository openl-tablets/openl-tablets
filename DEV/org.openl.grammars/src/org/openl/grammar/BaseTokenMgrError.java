package org.openl.grammar;

@SuppressWarnings("serial")
public class BaseTokenMgrError extends RuntimeException {

    boolean eOFSeen;
    int lexState;
    int errorLine;
    int errorColumn;
    String errorAfter;
    char curChar;
    int reason;

    public BaseTokenMgrError(String message) {
        super(message);
    }

    public BaseTokenMgrError() {
    }

    public BaseTokenMgrError(boolean eOFSeen,
            int lexState,
            int errorLine,
            int errorColumn,
            String errorAfter,
            char curChar,
            int reason,
            String lexicalError) {

        super(lexicalError);
        this.eOFSeen = eOFSeen;
        this.lexState = lexState;
        this.errorLine = errorLine;
        this.errorColumn = errorColumn;
        this.errorAfter = errorAfter;
        this.curChar = curChar;
        this.reason = reason;
    }

    public int getStartLine() {
        return errorLine;
    }

    public int getEndLine() {
        return errorLine;
    }

    public int getEndCol() {
        return eOFSeen ? errorColumn : errorColumn + 1;
    }

    public int getStartCol() {
        int len = errorAfter == null ? 0 : errorAfter.length();

        return errorColumn > 0 ? errorColumn - Math.min(len, errorColumn - 1) : 0;

    }

    @Override
    public String getMessage() {
        if (errorAfter == null || errorAfter.length() == 0) {
            return super.getMessage();
        }

        char c = errorAfter.charAt(0);
        if (!eOFSeen) {
            return ParserErrorMessage.printUnexpectedSymbolAfter(errorAfter, curChar);
        }

        switch (c) {
            case '\'':
            case '\"':
                return ParserErrorMessage.printNeedToClose(errorAfter, c);
        }

        return super.getMessage();
    }

}
