package org.openl.rules.calc;

import org.junit.Test;
import org.openl.types.java.JavaOpenClass;

public class CustomSpreadsheetResultOpenClassTest {
    @Test
    public void test() {
        CustomSpreadsheetResultOpenClass openClass = new CustomSpreadsheetResultOpenClass("CSR1", null, null);
        openClass.getField("$f1", false);
        openClass.addField(new CustomSpreadsheetResultField(null, "$f1", JavaOpenClass.OBJECT));
        openClass.addField(new CustomSpreadsheetResultField(null, "$F1", JavaOpenClass.OBJECT));
    }
}
