package org.openl.rules.ui.copy;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.tableeditor.renderkit.TableProperty.TablePropertyBuilder;

/**
 * @author Andrei Astrouski
 */
public class NewDimensionalVersionTableCopier extends TableCopier {

    public NewDimensionalVersionTableCopier(String tableUri) {
        super(tableUri);
        checkPropertiesExistance();
    }

    private void checkPropertiesExistance() {
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (propDefinition.isDimensional()
                    && getProperty(propDefinition.getName()) == null) {
                TableProperty property = new TablePropertyBuilder(propDefinition.getName(),
                        TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propDefinition.getName()))
                        .displayName(TablePropertyDefinitionUtils.getPropertyDisplayName(propDefinition.getName()))
                        .dimensional(true).build();
                getPropertiesManager().addProperty(property);
            }
        }
    }

    @Override
    public List<TableProperty> getPropertiesToDisplay() {
        List<TableProperty> properties = new ArrayList<TableProperty>();

        for (TableProperty property : getPropertiesManager().getProperties()) {
            if (property.isDimensional()) {
                properties.add(property);
            }
        }

        return properties;
    }

}
