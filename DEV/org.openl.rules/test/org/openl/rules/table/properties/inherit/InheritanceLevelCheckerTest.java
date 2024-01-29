package org.openl.rules.table.properties.inherit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InheritanceLevelCheckerTest {

    @Test
    public void testExistingName() {
        String propertyName = "name";

        boolean result = PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, propertyName);
        assertTrue(result);
    }
}
