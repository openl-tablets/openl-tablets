package org.openl.rules.tbasic;

public class PerformOperation extends RuntimeOperation {

    private Object openLStatement;

    @Override
    public Result execute(TBasicContext context, Object... objects) {
        // TODO Execute required OpenL statement
        // openLStatement.evaluate();
        return new Result(ReturnType.Next);
    }

}
