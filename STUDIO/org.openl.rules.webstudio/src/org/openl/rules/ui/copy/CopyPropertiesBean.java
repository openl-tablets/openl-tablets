package org.openl.rules.ui.copy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openl.rules.tableeditor.renderkit.TableProperty;

/**
 * Bean that handles property selection when a table is copied.
 *
 * @author PUdalau
 */
public class CopyPropertiesBean {
    private List<TableProperty> properties = new ArrayList<>();
    private final Set<String> possibleToAddProps;

    /**
     * Creates a property container with the supplied property names.
     *
     * @param possibleProperties List of property names that can be contained by this bean.
     */
    public CopyPropertiesBean(List<String> possibleProperties) {
        possibleToAddProps = new TreeSet<>(possibleProperties);
    }

    /**
     * @return List of table properties that has bean already added to bean.
     */
    public List<TableProperty> getProperties() {
        return properties;
    }

    /**
     * Set predefined properties into bean.
     *
     * @param properties List of properties.
     */
    public void setProperties(List<TableProperty> properties) {
        this.properties = properties;
        for (TableProperty property : properties) {
            possibleToAddProps.remove(property.getName());
        }
    }

    public List<String> getPossibleToAddProperties() {
        return new ArrayList<>(possibleToAddProps);
    }

    /**
     * Adds new property into the bean.
     */
    public void addProperty(TableProperty property) {
        properties.add(property);
        possibleToAddProps.remove(property.getName());
    }
}
