package org.openl.rules.dt;

import lombok.AccessLevel;
import lombok.Getter;

class SimpleReturnDTHeader extends DTHeader {

    @Getter(AccessLevel.PACKAGE)
    private final String title;

    SimpleReturnDTHeader(String statement, String title, int column, int row, int width) {
        super(new int[]{}, statement, column, row, width, width, false);
        this.title = title;
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

    @Override
    boolean isRule() {
        return false;
    }
}
