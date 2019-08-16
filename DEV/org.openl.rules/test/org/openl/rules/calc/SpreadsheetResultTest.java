package org.openl.rules.calc;

import org.junit.Assert;
import org.junit.Test;

public class SpreadsheetResultTest {

    @Test
    public void toStringTest() {
        SpreadsheetResult sr = new SpreadsheetResult();
        sr.setColumnNames(new String[] { "A", "B" });
        sr.setRowNames(new String[] { "C", "D" });
        sr.setResults(new Object[][] { { 1, "Text" }, { new int[] { 2, 4 }, new Double[] { 3.3, 4.7 } } });
        String text = sr.toString();
        Assert.assertEquals("-X- | A      | B         \nC   | 1      | Text      \nD   | [2, 4] | [3.3, 4.7]\n", text);
    }

    @Test
    public void testComparable() {
        // toPlain in SPR doesn't work with SortedSets
        Assert.assertFalse(Comparable.class.isAssignableFrom(SpreadsheetResult.class));
    }
}
