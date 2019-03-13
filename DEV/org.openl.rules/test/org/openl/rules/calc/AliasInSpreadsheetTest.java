package org.openl.rules.calc;

import org.junit.Test;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.rules.TestUtils;

public class AliasInSpreadsheetTest {

    @Test(expected = OutsideOfValidDomainException.class)
    public void test() {
        TestUtils.invoke("test/rules/calc1/AliasInSpreadsheet.xlsx", "test");
    }
}
