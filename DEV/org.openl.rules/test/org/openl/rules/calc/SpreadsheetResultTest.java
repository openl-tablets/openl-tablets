package org.openl.rules.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class SpreadsheetResultTest {

    @Test
    public void toStringTest() {
        SpreadsheetResult sr = new SpreadsheetResult();
        sr.setColumnNames(new String[] { "A", "B" });
        sr.setRowNames(new String[] { "C", "D" });
        sr.setResults(new Object[][] { { 1, "Text" }, { new int[] { 2, 4 }, new Double[] { 3.3, 4.7 } } });
        String text = sr.toString();
        assertEquals("-X- | A      | B         \nC   | 1      | Text      \nD   | [2, 4] | [3.3, 4.7]\n", text);
    }

    @Test
    public void testComparable() {
        // toPlain in SPR does not work with SortedSets
        assertFalse(Comparable.class.isAssignableFrom(SpreadsheetResult.class));
    }
}
