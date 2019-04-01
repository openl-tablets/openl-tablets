package org.openl.rules.ui.copy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableCopierTest {

    @Test
    public void testIsEmpty() {
        assertTrue(TableCopier.isEmpty(null));
        assertTrue(TableCopier.isEmpty(""));
        assertTrue(TableCopier.isEmpty(new Object[0]));
        assertFalse(TableCopier.isEmpty(Double.valueOf(12)));
        assertFalse(TableCopier.isEmpty(new int[] { 1, 2 }));
    }

}
