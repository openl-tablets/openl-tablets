package org.openl.rules.lang.xls.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TableUtilsTest {

    @Test
    public void testToCellURIe() {
        String actualUri = TableUtils.toCellURI("file:///foo.xlsx?sheet=Sheet1&range=A1:A22");
        assertEquals("file:///foo.xlsx?sheet=Sheet1&cell=A1", actualUri);

        actualUri = TableUtils.toCellURI("file:///foo.xlsx?range=A1:A22&sheet=Sheet1");
        assertEquals("file:///foo.xlsx?cell=A1&sheet=Sheet1", actualUri);

        actualUri = TableUtils.toCellURI("file:///foo.xlsx?sheet=Sheet1&cell=A1");
        assertEquals("file:///foo.xlsx?sheet=Sheet1&cell=A1", actualUri);

        actualUri = TableUtils.toCellURI("file:///foo.xlsx?range=A1:A22");
        assertEquals("file:///foo.xlsx?cell=A1", actualUri);

        actualUri = TableUtils.toCellURI("file:///foo.xlsx");
        assertEquals("file:///foo.xlsx", actualUri);

        actualUri = TableUtils.toCellURI("file:///foo.xlsx?sheet=%D0%9B%D0%B8%D1%81%D1%821&range=B29:B30");
        assertEquals("file:///foo.xlsx?sheet=%D0%9B%D0%B8%D1%81%D1%821&cell=B29", actualUri);
    }

    @Test
    public void testMakeTableId() {
        assertNull(TableUtils.makeTableId(null));

        String actualId = TableUtils.makeTableId("file:///foo.xlsx?sheet=%D0%9B%D0%B8%D1%81%D1%821&range=B29:B30");
        assertEquals("485c4459e4d5664002042594dc9d0cbd", actualId);

        actualId = TableUtils.makeTableId("file:///foo.xlsx?sheet=%D0%9B%D0%B8%D1%81%D1%821&cell=B29");
        assertEquals("485c4459e4d5664002042594dc9d0cbd", actualId);

    }

}
