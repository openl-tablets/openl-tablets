package org.openl.rules.table.properties.inherit;

import static org.junit.Assert.*;

import org.junit.Test;

public class InheritanceLevelCheckerTest {
    
    @Test
    public void testExistingName() {
        String propertyName = "name";
        
        boolean result = PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, propertyName);
        assertTrue(result);        
    }
    
    @Test
    public void testUnExistingName() {
        // property region unable to define on TABLE level for testing. it is defined in TablePropertyDefinition.xlsx
        String propertyName = "region";
        
        boolean result = PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, propertyName);
        assertFalse(result);
    }

}
