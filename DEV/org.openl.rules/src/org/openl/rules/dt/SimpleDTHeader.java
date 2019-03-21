package org.openl.rules.dt;

class SimpleDTHeader extends DTHeader {
    
    int row;
    boolean horizontal;
    String title;
    
    SimpleDTHeader(int methodParameterIndex, String statement, String title, int column, int width) {
        super(new int[] { methodParameterIndex }, statement, column, width);
        this.title = title;
        this.horizontal = false;
    }
    
    SimpleDTHeader(int methodParameterIndex, String statement, int column, int row) {
        super(new int[] { methodParameterIndex }, statement, column, 1);
        this.horizontal = true;
        this.row = row;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getRow() {
        return row;
    }
    
    @Override
    boolean isHCondition() {
        return horizontal;
    }

    @Override
    boolean isCondition() {
        return true;
    }
    
    @Override
    boolean isAction() {
        return false;
    }

    @Override
    boolean isReturn() {
        return false;
    }
}
