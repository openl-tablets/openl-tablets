package org.openl.rules.table.properties.def;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

public class DefaultPropertyDefinitionsTest {
    
    @Test
    public void dimensionalPropertiesCategoryTest() {
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (tablePropertyDefinition.isDimensional()) {
                InheritanceLevel[] inheritanceLevels = tablePropertyDefinition.getInheritanceLevel();
                Set<InheritanceLevel> set = new HashSet<>();
                for (InheritanceLevel inheritanceLevel : inheritanceLevels) {
                    set.add(inheritanceLevel);
                }
                if (!set.contains(InheritanceLevel.CATEGORY)) {
                    Assert.fail("All dimensional properties must contains CATEGORY inheritance level.");
                }
                if (!set.contains(InheritanceLevel.MODULE)) {
                    Assert.fail("All dimensional properties must contains MODULE inheritance level.");
                }
                if (!set.contains(InheritanceLevel.TABLE)) {
                    Assert.fail("All dimensional properties must contains TABLE inheritance level.");
                }
            }
        }
    }
}
