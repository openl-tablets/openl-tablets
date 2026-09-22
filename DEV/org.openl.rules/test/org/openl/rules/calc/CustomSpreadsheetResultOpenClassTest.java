package org.openl.rules.calc;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

class CustomSpreadsheetResultOpenClassTest {
    @Test
    void fieldsDifferingOnlyInCaseCoexistAfterALookup() {
        var openClass = new CustomSpreadsheetResultOpenClass("CSR1",
                null,
                null,
                true);
        openClass.getField("$f1", true);
        var lowerCase = new CustomSpreadsheetResultField(null, "$f1", JavaOpenClass.OBJECT);
        var upperCase = new CustomSpreadsheetResultField(null, "$F1", JavaOpenClass.OBJECT);

        openClass.addField(lowerCase);
        openClass.addField(upperCase);

        assertSame(lowerCase, openClass.getField("$f1", true));
        assertSame(upperCase, openClass.getField("$F1", true));
    }

    @Test
    void testIsAssignableFromNullOpenClass() {
        var openClass = new CustomSpreadsheetResultOpenClass("CSR1",
                null,
                null,
                true);
        assertFalse(openClass.isAssignableFrom(NullOpenClass.the));
    }
}
