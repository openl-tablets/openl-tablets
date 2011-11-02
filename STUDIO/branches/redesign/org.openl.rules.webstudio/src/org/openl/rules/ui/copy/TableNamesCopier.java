package org.openl.rules.ui.copy;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.tableeditor.renderkit.TableProperty.TablePropertyBuilder;

public class TableNamesCopier extends TableCopier {

    public TableNamesCopier(String tableUri) {
        super(tableUri);
        checkPropertiesExistance();
    }

    private void checkPropertiesExistance() {
        TableProperty nameProperty = getProperty(TableBuilder.TABLE_PROPERTIES_NAME);
        if (nameProperty == null) {
            // Property "name" is absent in base table
            nameProperty = new TablePropertyBuilder(TableBuilder.TABLE_PROPERTIES_NAME, 
                    TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(TableBuilder.TABLE_PROPERTIES_NAME))
                    .displayName(TablePropertyDefinitionUtils.getPropertyDisplayName(TableBuilder.TABLE_PROPERTIES_NAME))
                    .build();
            getPropertiesManager().addProperty(nameProperty);
        }
    }

    /*private void validateTechnicalName(TableSyntaxNode node) throws CreateTableException {
        String[] headerStr = node.getHeaderLineValue().getValue().split(" ");
        if (headerStr.length >=3) {
            String existingTechnicalName = headerStr[2].substring(0, headerStr[2].indexOf("("));
            if (tableTechnicalName.equalsIgnoreCase(existingTechnicalName)) {
                throw new CreateTableException("Table with the same technical name already exists");
            }
        }
    }*/

    @Override
    public List<TableProperty> getPropertiesToDisplay() {
        List<TableProperty> properties = new ArrayList<TableProperty>();
        properties.add(getProperty(TableBuilder.TABLE_PROPERTIES_NAME));
        return properties;
    }

}
