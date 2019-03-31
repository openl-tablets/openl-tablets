package org.openl.rules.webstudio.web.tableeditor;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class EditHelper {

    private EditHelper() {
    }

    public static boolean updateSystemProperties(IOpenLTable table,
            TableEditorModel tableEditorModel,
            String userMode) {
        boolean result = true;
        if (table.isCanContainProperties()) {
            List<TablePropertyDefinition> systemPropertiesDefinitions = TablePropertyDefinitionUtils
                .getSystemProperties();
            for (TablePropertyDefinition systemProperty : systemPropertiesDefinitions) {
                result = updateSystemValue(tableEditorModel, systemProperty, userMode);
            }
        }
        return result;
    }

    private static boolean updateSystemValue(TableEditorModel editorModel,
            TablePropertyDefinition systemProperty,
            String userMode) {
        final Logger log = LoggerFactory.getLogger(EditHelper.class);
        boolean result = false;
        String systemValueDescriptor = systemProperty.getSystemValueDescriptor();

        if (userMode.equals("single") && systemValueDescriptor.equals(SystemValuesManager.CURRENT_USER_DESCRIPTOR)) {
            return true;
        }

        if (systemProperty.getSystemValuePolicy().equals(SystemValuePolicy.ON_EACH_EDIT)) {
            Object systemValue = SystemValuesManager.getInstance().getSystemValue(systemValueDescriptor);
            if (systemValue != null) {
                try {
                    if (editorModel != null) {
                        editorModel.setProperty(systemProperty.getName(), systemValue);
                        result = true;
                    }
                } catch (Exception e) {
                    String message = String.format("Can`t update system property '%s' with value '%s'",
                        systemProperty.getName(),
                        systemValue);
                    log.error(message, e);
                    throw new IllegalStateException(message, e);
                }
            }
        }
        return result;
    }
}
