package org.openl.rules.calc;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SpreadsheetSymbols {

    /**
     * cell name indicating return statement
     */
    TYPE_DELIMITER(":"),
    TILDE("~"),
    ASTERISK("*");

    private final String symbols;

    @Override
    public String toString() {
        return symbols;
    }

}
