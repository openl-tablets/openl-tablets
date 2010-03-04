package org.openl.rules.table.properties.inherit;

import static org.junit.Assert.*;

import org.junit.Test;

public class InheritanceLevelCheckerTest {
    
    @Test
    public void testExistingName() {
        String propertyName = "name";
        try {
            InheritanceLevelChecker.checkPropertyLevel(InheritanceLevel.TABLE, propertyName);
            assertTrue(true);
        } catch (InvalidPropertyLevelException ex) {
            fail();
        }
    }
    
    @Test
    public void testUnExistingName() {
        // property region unable to define on TABLE level for testing. it is defined in TablePropertyDefinition.xlsx
        String propertyName = "region";
        try {
            InheritanceLevelChecker.checkPropertyLevel(InheritanceLevel.TABLE, propertyName);
            fail();
        } catch (InvalidPropertyLevelException ex) {
            assertTrue(true);
        }
    }

}
