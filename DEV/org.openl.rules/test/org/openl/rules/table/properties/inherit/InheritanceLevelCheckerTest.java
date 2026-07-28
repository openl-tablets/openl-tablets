package org.openl.rules.table.properties.inherit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InheritanceLevelCheckerTest {

    @Test
    void testExistingName() {
        var propertyName = "name";

        var result = PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, propertyName);
        assertTrue(result);
    }
}
