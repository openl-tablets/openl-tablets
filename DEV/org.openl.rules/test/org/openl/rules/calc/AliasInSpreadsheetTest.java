package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.rules.TestUtils;

public class AliasInSpreadsheetTest {

    @Test
    public void test() {
        assertThrows(OutsideOfValidDomainException.class, () -> {
            TestUtils.invoke("test/rules/calc1/AliasInSpreadsheet.xlsx", "test");
        });
    }
}
