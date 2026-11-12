package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

/**
 * Checks the error of a column title merged over several columns.
 *
 * @author Yury Molchan
 */
class MergedColumnTitleTest {

    @Test
    void mergedTitleIsReportedOnItsCell() {
        var errors = TestUtils.collectErrorMessagesFromFileProcessing("test/rules/data/MergedColumnTitle.xlsx");

        assertEquals(1, errors.length);
        assertEquals("The title 'Age and factor' is merged over several columns. Each column must have its own title.",
                errors[0].getSummary());
        assertTrue(errors[0].getSourceLocation().endsWith("?sheet=Intro&range=C10:D10"), errors[0].getSourceLocation());
    }
}
