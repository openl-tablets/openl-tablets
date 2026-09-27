package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SpreadsheetResultTest {

    @Test
    void toStringTest() {
        var sr = new SpreadsheetResult();
        sr.setColumnNames(new String[]{"A", "B"});
        sr.setRowNames(new String[]{"C", "D"});
        sr.setResults(new Object[][]{{1, "Text"}, {new int[]{2, 4}, new Double[]{3.3, 4.7}}});
        var text = sr.toString();
        assertEquals("-X- | A      | B         \nC   | 1      | Text      \nD   | [2, 4] | [3.3, 4.7]\n", text);
    }

    @Test
    void testComparable() {
        // toPlain in SPR does not work with SortedSets
        assertFalse(Comparable.class.isAssignableFrom(SpreadsheetResult.class));
    }

    @Test
    void toMapNumbersRepeatedNames() {
        var sr = new SpreadsheetResult(new Object[][]{{1}, {2}, {3}},
                new String[]{"R1", "R2", "R3"},
                new String[]{"C"},
                new String[]{"Total", "Total", "Total"},
                new String[]{"C"},
                Map.of());
        assertEquals(Map.of("Total", 1, "Total1", 2, "Total2", 3), sr.toMap(false, null));
    }

    @Test
    void toMapKeepsANameTakenByNull() {
        var sr = new SpreadsheetResult(new Object[][]{{null}, {2}},
                new String[]{"R1", "R2"},
                new String[]{"C"},
                new String[]{"Total", "Total"},
                new String[]{"C"},
                Map.of());
        var map = sr.toMap(false, null);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("Total"));
        assertNull(map.get("Total"));
        assertEquals(2, map.get("Total1"));
    }
}
