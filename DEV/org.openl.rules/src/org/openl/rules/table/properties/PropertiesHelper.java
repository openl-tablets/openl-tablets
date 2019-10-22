package org.openl.rules.table.properties;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodDelegator;

public class PropertiesHelper {

    public static final String PROPERTIES_HEADER = "properties";

    private PropertiesHelper() {
    }

    public static ILogicalTable getPropertiesTableSection(ILogicalTable table) {

        if (table.getHeight() < 2) {
            return null;
        }

        ILogicalTable propTable = table.getRows(1, 1);
        String header = propTable.getSource().getCell(0, 0).getStringValue();

        if (!PROPERTIES_HEADER.equals(header)) {
            return null;
        }

        return propTable.getColumns(1);
    }

    public static ITableProperties getTableProperties(IOpenMethod method) {
        if (method instanceof OpenMethodDispatcher) {
            List<IOpenMethod> methods = ((OpenMethodDispatcher) method).getCandidates();
            if (methods.size() == 1) {
                return getTableProperties(methods.get(0));
            } else {
                throw new IllegalArgumentException(
                    "Dispatcher method with more than one candidate does not have properties.");
            }
        }

        if (method instanceof ITablePropertiesMethod) {
            return ((ITablePropertiesMethod) method).getMethodProperties();
        } else if (method.getInfo() != null) {
            TableProperties properties = new TableProperties();
            Map<String, Object> definedInTable = method.getInfo().getProperties();
            if (definedInTable != null) {
                for (Entry<String, Object> property : definedInTable.entrySet()) {
                    properties.setFieldValue(property.getKey(), property.getValue());
                }
            }
            return properties;

        }
        if (method instanceof MethodDelegator) {
            return getTableProperties(((MethodDelegator) method).getMethod());
        }

        return new TableProperties();
    }

    public static Map<String, Object> getMethodProperties(IOpenMethod method) {
        if (method instanceof ITablePropertiesMethod) {
            return ((ITablePropertiesMethod) method).getProperties();
        } else if (method.getInfo() != null) {
            return method.getInfo().getProperties();
        } else if (method instanceof MethodDelegator) {
            return getMethodProperties(method.getMethod());
        } else {
            return null;
        }
    }
}
