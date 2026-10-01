package org.openl.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldMetaInfoTest {

    @Test
    void aFieldHasNoTableProperties() {
        var info = new FieldMetaInfo("String", "name", null, "file:/rules.xlsx");

        assertTrue(info.getProperties().isEmpty());
        assertEquals("String name", info.getDisplayName(0));
    }
}
