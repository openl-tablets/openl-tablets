package org.openl.rules.dt;

public class SimpleReturnDTHeader extends DTHeader {

    String title;
    
    SimpleReturnDTHeader(String statement, String title, int column) {
        super(new int[] {}, statement, column);
        this.title = title;
    }
    
    public String getTitle() {
        return title;
    }
    
    @Override
    int getNumberOfUsedColumns() {
        return 1;
    }

    @Override
    boolean isCondition() {
        return false;
    }
    
    @Override
    boolean isAction() {
        return false;
    }

    @Override
    boolean isReturn() {
        return true;
    }
}
