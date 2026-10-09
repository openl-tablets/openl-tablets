package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class XlsSheetShiftsTest {

    @Test
    void isMoved_tellsEditsInPlaceFromInsertedAndDeletedElements() {
        assertFalse(XlsSheetShifts.isMoved(new long[]{1, 2, 3}, new long[]{1, 9, 3}));
        assertFalse(XlsSheetShifts.isMoved(new long[]{1, 2, 0}, new long[]{1, 2, 3}));
        assertFalse(XlsSheetShifts.isMoved(new long[]{7, 1, 1}, new long[]{1, 1, 1}));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 2, 0}, new long[]{1, 9, 2}));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 9, 2}, new long[]{1, 2, 0}));
        assertTrue(XlsSheetShifts.isMoved(new long[]{1, 0, 2}, new long[]{1, 2, 0}));
    }

    @Test
    void isMoved_takesTooManyChangesAsMove() {
        var base = new long[10_000];
        var other = new long[10_000];
        for (var i = 0; i < base.length; i++) {
            base[i] = i;
            other[i] = -i - 1;
        }
        assertTrue(XlsSheetShifts.isMoved(base, other));
    }
}
