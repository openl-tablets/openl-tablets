package org.openl.rules.dt;

class SimpleReturnDTHeader extends DTHeader {

    String title;

    SimpleReturnDTHeader(String statement, String title, int column, int width) {
        super(new int[] {}, statement, column, width);
        this.title = title;
    }

    String getTitle() {
        return title;
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
