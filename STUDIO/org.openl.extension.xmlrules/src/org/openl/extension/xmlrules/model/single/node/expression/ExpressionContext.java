package org.openl.extension.xmlrules.model.single.node.expression;

public class ExpressionContext {
    private static ThreadLocal<ExpressionContext> instanceHolder = new ThreadLocal<ExpressionContext>();

    private final boolean arrayExpression;

    private final int startRow;
    private final int startColumn;
    private final int endRow;
    private final int endColumn;

    private int currentRow;
    private int currentColumn;

    private boolean outArray = false;
    private boolean canHandleArrayOperators = true;

    public static ExpressionContext getInstance() {
        return instanceHolder.get();
    }

    public static void setInstance(ExpressionContext instance) {
        instanceHolder.set(instance);
    }

    public static void removeInstance() {
        instanceHolder.remove();
    }

    public ExpressionContext() {
        arrayExpression = false;
        startRow = endRow = startColumn = endColumn = -1;

        currentRow = startRow;
        currentColumn = startColumn;
    }

    public ExpressionContext(int startRow, int startColumn, int endRow, int endColumn) {
        arrayExpression = true;
        this.startRow = startRow;
        this.startColumn = startColumn;
        this.endRow = endRow;
        this.endColumn = endColumn;

        currentRow = startRow;
        currentColumn = startColumn;
    }

    public boolean isArrayExpression() {
        return arrayExpression;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public void setCurrentRow(int currentRow) {
        this.currentRow = currentRow;
    }

    public int getCurrentColumn() {
        return currentColumn;
    }

    public void setCurrentColumn(int currentColumn) {
        this.currentColumn = currentColumn;
    }

    public boolean isOutArray() {
        return outArray;
    }

    public void setOutArray(boolean outArray) {
        this.outArray = outArray;
    }

    public boolean isCanHandleArrayOperators() {
        return canHandleArrayOperators;
    }

    public void setCanHandleArrayOperators(boolean canHandleArrayOperators) {
        this.canHandleArrayOperators = canHandleArrayOperators;
    }
}
