package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class StubSpreadSheetResultTest {

    @Test
    void stubsWithTheSameFieldValuesAreEqual() {
        var stub = stub("Total", 10);
        var same = stub("Total", 10);

        assertEquals(stub, same);
        assertEquals(stub.hashCode(), same.hashCode());
    }

    @Test
    void stubsWithDifferentFieldValuesAreNotEqual() {
        var stub = stub("Total", 10);

        assertNotEquals(stub, stub("Total", 20));
        assertNotEquals(stub, stub("Sum", 10));
        assertNotEquals(stub, new SpreadsheetResult());
    }

    private static StubSpreadSheetResult stub(String field, Object value) {
        var stub = new StubSpreadSheetResult();
        stub.setFieldValue(field, value);
        return stub;
    }
}
