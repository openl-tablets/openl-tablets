package org.openl.rules.table.properties.def;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.lang.xls.XlsNodeTypes;
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
                    Assert.fail("All dimensional properties must have CATEGORY inheritance level.");
                }
                if (!set.contains(InheritanceLevel.MODULE)) {
                    Assert.fail("All dimensional properties must have MODULE inheritance level.");
                }
                if (!set.contains(InheritanceLevel.TABLE)) {
                    Assert.fail("All dimensional properties must have TABLE inheritance level.");
                }
            }
        }
    }

    @Test
    public void dimensionalPropertiesNodeTypeTest() {
        Set<XlsNodeTypes> dimensionalPropertiesNodeTypes = new HashSet<>();
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_DT);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_SPREADSHEET);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_TBASIC);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_COLUMN_MATCH);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_METHOD);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_PROPERTIES);
        final String failMessage = "All dimensional properties must have XLS_DT, XLS_SPREADSHEET, XLS_TBASIC, XLS_COLUMN_MATCH, XLS_METHOD, XLS_PROPERTIES only in table types.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (tablePropertyDefinition.isDimensional()) {
                Set<XlsNodeTypes> set = new HashSet<>();
                for (XlsNodeTypes xlsNodeType : tablePropertyDefinition.getTableType()) {
                    set.add(xlsNodeType);
                }
                set.retainAll(dimensionalPropertiesNodeTypes);
                if (set.size() != dimensionalPropertiesNodeTypes.size()) {
                    Assert.fail(failMessage);
                }
            }
        }
    }

    @Test
    public void infoPropertiesInheritanceTypeTest() {
        final String failMessage = "All info properties must have TABLE inheritance level.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if ("Info".equalsIgnoreCase(tablePropertyDefinition.getGroup())) {
                if (tablePropertyDefinition.getInheritanceLevel().length == 0) {
                    Assert.fail(failMessage);
                } else {
                    boolean found = false;
                    for (InheritanceLevel inheritanceLevel : tablePropertyDefinition.getInheritanceLevel()) {
                        if (InheritanceLevel.TABLE.equals(inheritanceLevel)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Assert.fail(failMessage);
                    }
                }
            }
        }
    }

    @Test
    public void versionPropertiesInheritanceTypeTest() {
        final String failMessage = "All version properties must have TABLE inheritance level only.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if ("Version".equalsIgnoreCase(tablePropertyDefinition.getGroup())) {
                if (tablePropertyDefinition.getInheritanceLevel().length != 1) {
                    Assert.fail(failMessage);
                } else {
                    if (!InheritanceLevel.TABLE.equals(tablePropertyDefinition.getInheritanceLevel()[0])) {
                        Assert.fail(failMessage);
                    }
                }
            }
        }
    }
    
    @Test
    public void versionPropertiesNodeTypeTest() {
        Set<XlsNodeTypes> dimensionalPropertiesNodeTypes = new HashSet<>();
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_DT);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_SPREADSHEET);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_TBASIC);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_COLUMN_MATCH);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_METHOD);
        final String failMessage = "All dimensional properties must have XLS_DT, XLS_SPREADSHEET, XLS_TBASIC, XLS_COLUMN_MATCH, XLS_METHOD only in table types.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if ("Version".equalsIgnoreCase(tablePropertyDefinition.getGroup())) {
                Set<XlsNodeTypes> set = new HashSet<>();
                for (XlsNodeTypes xlsNodeType : tablePropertyDefinition.getTableType()) {
                    set.add(xlsNodeType);
                }
                set.retainAll(dimensionalPropertiesNodeTypes);
                if (set.size() != dimensionalPropertiesNodeTypes.size()) {
                    Assert.fail(failMessage);
                }
            }
        }
    }

}
