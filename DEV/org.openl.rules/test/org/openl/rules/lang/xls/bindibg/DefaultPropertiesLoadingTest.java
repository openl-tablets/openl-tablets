package org.openl.rules.lang.xls.bindibg;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

public class DefaultPropertiesLoadingTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/DefaultPropertiesLoadingTest.xls";

    public DefaultPropertiesLoadingTest() {
        super(SRC);
    }

    @Test
    public void testLoadingDefaultValuesForPreviouslyEmptyProp() {
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {

            assertEquals("Check that number of properties defined in table is 0", resultTsn.getTableProperties()
                    .getTableProperties().size(), 0);

            assertTrue("Tsn doesn`t have properties defined in appropriate table in excel",
                    !resultTsn.hasPropertiesDefinedInTable());

            List<String> tsnPropNames = getAllTableProperties(resultTsn);

            // FIXME: defaultPropDefinitionsNames is always empty because of a bug in getPropertiesToBeSetByDefault() but test doesn't fail
            // FIXME: What does this test check?

            List<String> defaultPropDefinitionsNames = getDefaultPropDefinitions(resultTsn);
            assertTrue("Tsn contains all properties that must be set by default for this type of table",
                    tsnPropNames.containsAll(defaultPropDefinitionsNames));
        } else {
            fail();
        }
    }

    private List<String> getDefaultPropDefinitions(TableSyntaxNode tsn) {
        List<String> defaultPropDefinitionsNames = new ArrayList<String>();
        List<TablePropertyDefinition> defaultPropDefinitions = TablePropertyDefinitionUtils
                .getPropertiesToBeSetByDefault(tsn.getType());
        for (TablePropertyDefinition dataPropertyDefinition : defaultPropDefinitions) {
            defaultPropDefinitionsNames.add(dataPropertyDefinition.getName());
        }
        return defaultPropDefinitionsNames;
    }

    private List<String> getAllTableProperties(TableSyntaxNode tsn) {
        List<String> tsnPropNames = new ArrayList<String>();
        for (Map.Entry<String, Object> property : tsn.getTableProperties().getAllProperties().entrySet()) {
            tsnPropNames.add(property.getKey());
        }
        return tsnPropNames;
    }

}
