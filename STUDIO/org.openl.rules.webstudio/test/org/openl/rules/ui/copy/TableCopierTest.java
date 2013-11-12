package org.openl.rules.ui.copy;

import static org.junit.Assert.*;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class TableCopierTest {
    
    @Test
    public void testIsEmpty() {
        assertTrue(TableCopier.isEmpty(null));
        assertTrue(TableCopier.isEmpty(StringUtils.EMPTY));
        assertTrue(TableCopier.isEmpty(new Object[0]));
        assertFalse(TableCopier.isEmpty(Double.valueOf(12)));
    }

}
