package org.openl.rules.calc;

import org.junit.Test;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.rules.BaseOpenlBuilderHelper;

public class AliasInSpreadsheetTest extends BaseOpenlBuilderHelper {

    private static String SRC = "test/rules/calc1/AliasInSpreadsheet.xlsx";

    public AliasInSpreadsheetTest() {
        super(SRC);
    }

    @Test(expected = OutsideOfValidDomainException.class)
    public void test() {
        invokeMethod("test");
    }

}
