package org.openl.rules.lang.xls.binding;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.StringValue;
import org.openl.rules.table.ILogicalTable;

public class TableProperties {
    static public class Property {
        StringValue key;
        StringValue value;

        public Property(StringValue key, StringValue value) {
            this.key = key;
            this.value = value;
        }

        public StringValue getKey() {
            return key;
        }

        public StringValue getValue() {
            return value;
        }

        public void setValue(StringValue value) {
            this.value = value;
        }
    }

    ILogicalTable table;

    Property[] properties;

    public TableProperties(ILogicalTable table, Property[] properties) {
        this.table = table;
        this.properties = properties;
    }

    public Property[] getProperties() {
        return properties;
    }

    public Property getProperty(String key) {
        for (int i = 0; i < properties.length; i++) {
            Property p = properties[i];

            if (p.key.getValue().equals(key)) {
                return p;
            }
        }
        return null;
    }

    public String getPropertyValue(String key) {
        Property p = getProperty(key);
        return p == null ? null : p.getValue().getValue();
    }

    public ILogicalTable getTable() {
        return table;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    public void setTable(ILogicalTable table) {
        this.table = table;
    }

    public void setProperty(String name, String value) {
        if (name != null) {
            Property property = getProperty(name);
            if (property != null) {
                property.setValue(new StringValue(value == null ? "" : value));
            } else {
                Property newProperty = new Property(new StringValue(name), new StringValue(value));
                List<Property> propList = asList();
                propList.add(newProperty);
                setProperties(propList.toArray(new Property[]{}));
            }
        }
    }

    public List<Property> asList() {
        List<Property> propList = new ArrayList<Property>();
        for (Property prop : properties) {
            propList.add(prop);
        }
        return propList;
    }
}
