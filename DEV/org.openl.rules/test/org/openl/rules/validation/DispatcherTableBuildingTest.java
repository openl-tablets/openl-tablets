package org.openl.rules.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

public class DispatcherTableBuildingTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/overload/DispatcherTest.xlsx";

    public DispatcherTableBuildingTest() {
        super(SRC);
    }

    @Test
    public void checkKeywordsInSignature() {
        assertNotNull(findDispatcherForMethod("arraysTest"));
        assertNotNull(findDispatcherForMethod("keywordsTest"));
        assertEquals(0, getCompiledOpenClass().getAllMessages().size());
    }
}
