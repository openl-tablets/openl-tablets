package org.openl.rules.table.properties.def;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

class DefaultPropertyDefinitionsTest {

    @Test
    void dimensionalPropertiesCategoryTest() {
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (tablePropertyDefinition.isDimensional()) {
                var inheritanceLevels = tablePropertyDefinition.getInheritanceLevel();
                var set = new HashSet<InheritanceLevel>(Arrays.asList(inheritanceLevels));
                if (!set.contains(InheritanceLevel.CATEGORY)) {
                    fail("All dimensional properties must have CATEGORY inheritance level.");
                }
                if (!set.contains(InheritanceLevel.MODULE)) {
                    fail("All dimensional properties must have MODULE inheritance level.");
                }
                if (!set.contains(InheritanceLevel.TABLE)) {
                    fail("All dimensional properties must have TABLE inheritance level.");
                }
            }
        }
    }

    @Test
    void dimensionalPropertiesNodeTypeTest() {
        var dimensionalPropertiesNodeTypes = new HashSet<XlsNodeTypes>();
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_DT);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_SPREADSHEET);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_TBASIC);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_COLUMN_MATCH);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_METHOD);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_PROPERTIES);
        final var failMessage = "All dimensional properties must have XLS_DT, XLS_SPREADSHEET, XLS_TBASIC, XLS_COLUMN_MATCH, XLS_METHOD, XLS_PROPERTIES only in table types.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (tablePropertyDefinition.isDimensional()) {
                var set = new HashSet<XlsNodeTypes>(Arrays.asList(tablePropertyDefinition.getTableType()));
                set.retainAll(dimensionalPropertiesNodeTypes);
                if (set.size() != dimensionalPropertiesNodeTypes.size()) {
                    fail(failMessage);
                }
            }
        }
    }

    @Test
    void infoPropertiesInheritanceTypeTest() {
        final var failMessage = "All info properties must have TABLE inheritance level.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if ("Info".equalsIgnoreCase(tablePropertyDefinition.getGroup())) {
                if (tablePropertyDefinition.getInheritanceLevel().length == 0) {
                    fail(failMessage);
                } else {
                    var found = false;
                    for (InheritanceLevel inheritanceLevel : tablePropertyDefinition.getInheritanceLevel()) {
                        if (InheritanceLevel.TABLE.equals(inheritanceLevel)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        fail(failMessage);
                    }
                }
            }
        }
    }

    @Test
    void versionPropertiesInheritanceTypeTest() {
        final var failMessage = "All version properties must have TABLE inheritance level only.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if ("Version".equalsIgnoreCase(tablePropertyDefinition.getGroup())) {
                if (tablePropertyDefinition.getInheritanceLevel().length != 1) {
                    fail(failMessage);
                } else {
                    if (!InheritanceLevel.TABLE.equals(tablePropertyDefinition.getInheritanceLevel()[0])) {
                        fail(failMessage);
                    }
                }
            }
        }
    }

    @Test
    void versionPropertiesNodeTypeTest() {
        var dimensionalPropertiesNodeTypes = new HashSet<XlsNodeTypes>();
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_DT);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_SPREADSHEET);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_TBASIC);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_COLUMN_MATCH);
        dimensionalPropertiesNodeTypes.add(XlsNodeTypes.XLS_METHOD);
        final var failMessage = "All dimensional properties must have XLS_DT, XLS_SPREADSHEET, XLS_TBASIC, XLS_COLUMN_MATCH, XLS_METHOD only in table types.";
        for (TablePropertyDefinition tablePropertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if ("Version".equalsIgnoreCase(tablePropertyDefinition.getGroup())) {
                var set = new HashSet<XlsNodeTypes>(Arrays.asList(tablePropertyDefinition.getTableType()));
                set.retainAll(dimensionalPropertiesNodeTypes);
                if (set.size() != dimensionalPropertiesNodeTypes.size()) {
                    fail(failMessage);
                }
            }
        }
    }

}
