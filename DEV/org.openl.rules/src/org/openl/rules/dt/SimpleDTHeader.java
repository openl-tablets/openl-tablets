package org.openl.rules.dt;

class SimpleDTHeader extends DTHeader {

    boolean horizontal;
    String title;
    
    SimpleDTHeader(int methodParameterIndex, String statement, String title, int column, boolean horizontal) {
        super(new int[] { methodParameterIndex }, statement, column);
        this.title = title;
        this.horizontal = horizontal;
    }
    
    public String getTitle() {
        return title;
    }
    
    public boolean isHorizontal() {
        return horizontal;
    }

    @Override
    int getNumberOfUsedColumns() {
        return 1;
    }

    @Override
    boolean isCondition() {
        return true;
    }

    @Override
    boolean isReturn() {
        return false;
    }
}
