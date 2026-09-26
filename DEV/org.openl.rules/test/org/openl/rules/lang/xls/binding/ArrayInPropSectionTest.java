package org.openl.rules.lang.xls.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

class ArrayInPropSectionTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/TestArrayInPropSection.xls";

    public ArrayInPropSectionTest() {
        super(SRC);
    }

    @Test
    void testLoadingArrayInPropertyTableSection() {
        final var tableName = "Rules DoubleValue driverRiskScoreOverloadTest(String driverRisk)";
        var resultTsn = findTable(tableName);

        if (resultTsn != null) {
            assertEquals(4,
                    resultTsn.getTableProperties().getTableProperties().size(),
                    "Check that number of properties defined in table is 4");
            assertEquals("tag1", resultTsn.getTableProperties().getTags()[0]);
            assertEquals("tag3", resultTsn.getTableProperties().getTags()[1]);
            assertEquals("tag4", resultTsn.getTableProperties().getTags()[2]);
        } else {
            fail();
        }
    }

}
