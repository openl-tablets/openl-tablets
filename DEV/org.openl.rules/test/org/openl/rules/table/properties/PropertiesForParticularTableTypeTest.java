package org.openl.rules.table.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.enumeration.RegionsEnum;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

public class PropertiesForParticularTableTypeTest extends BaseOpenlBuilderHelper {

    private static final String __src = "test/rules/PropertiesForParticularTableType.xls";

    public PropertiesForParticularTableTypeTest() {
        super(__src);
    }

    @Test
    public void testErrorParsing() {
        String tableName = "Rules void hello1(int hour)";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            ITableProperties tableProperties = resultTsn.getTableProperties();
            assertNotNull(tableProperties);

            assertEquals(7, tableProperties.getAllProperties().size());

        } else {
            fail();
        }
    }

    @Test
    public void testNotProcessingInheritPropertiesForTableType() {
        String tableName = "Rules void hello2(int hour)";
        TableSyntaxNode resultTsn = findTable(tableName);
        if (resultTsn != null) {
            ITableProperties tableProperties = resultTsn.getTableProperties();
            assertNotNull(tableProperties);

            Map<String, Object> categoryProperties = tableProperties.getCategoryProperties();
            assertEquals(5, categoryProperties.size());
            // check that we have all properties from category level
            assertEquals(InheritanceLevel.CATEGORY.getDisplayName(), categoryProperties.get("scope"));
            assertEquals("My Category", categoryProperties.get("category"));
            assertEquals("newLob", ((String[]) categoryProperties.get("lob"))[0]);
            assertEquals(UsRegionsEnum.SE.name(), ((UsRegionsEnum[]) categoryProperties.get("usregion"))[0].name());
            assertEquals(RegionsEnum.NCSA.name(), ((RegionsEnum[]) categoryProperties.get("region"))[0].name());

            Map<String, Object> allProperties = tableProperties.getAllProperties();
            assertEquals(10,
                    allProperties.size(),
                    "AllProperties size is 10, ignore property 'scope' and including default properties");
            assertFalse(
                    allProperties.containsKey("scope"),
                    "There is no property 'scope' applied for this table, as it cannot be defined in such table type");

        } else {
            fail();
        }
    }

}
