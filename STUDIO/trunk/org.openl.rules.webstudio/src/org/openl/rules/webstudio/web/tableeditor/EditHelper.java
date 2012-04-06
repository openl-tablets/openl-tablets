package org.openl.rules.webstudio.web.tableeditor;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.webstudio.properties.SystemValuesManager;

public final class EditHelper {
	
	private static final Log LOG = LogFactory.getLog(EditHelper.class);
	
	private EditHelper(){}
	
	public static boolean updateSystemProperties(IOpenLTable table, TableEditorModel tableEditorModel) {
        boolean result = true;
        if (table.isCanContainProperties()) {
            List<TablePropertyDefinition> systemPropertiesDefinitions = TablePropertyDefinitionUtils.getSystemProperties();
            for (TablePropertyDefinition systemProperty : systemPropertiesDefinitions) {
                result = updateSystemValue(tableEditorModel, systemProperty);
            }
        } 
        return result;
    } 
	
	private static boolean updateSystemValue(TableEditorModel editorModel, TablePropertyDefinition systemProperty) {
        boolean result = false;
        Object systemValue = null;

        if (systemProperty.getSystemValuePolicy().equals(SystemValuePolicy.ON_EACH_EDIT)) {
            systemValue = SystemValuesManager.getInstance().getSystemValue(systemProperty.getSystemValueDescriptor());
            if (systemValue != null) {
                try {
                	if (editorModel != null) {
                		editorModel.setProperty(systemProperty.getName(), systemValue);
                        result = true;
                	}                    
                } catch (Exception e) {
                    LOG.error(String.format("Can`t update system property '%s' with value '%s'", systemProperty.getName(),
                            systemValue), e);
                }
            }
        }
        return result;
    }
}
