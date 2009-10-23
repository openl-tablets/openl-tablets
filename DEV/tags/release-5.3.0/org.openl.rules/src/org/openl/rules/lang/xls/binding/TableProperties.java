package org.openl.rules.lang.xls.binding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;

public class TableProperties {
    static public class Property {
        StringValue key;
        ObjectValue value;

        public Property(StringValue key, ObjectValue value) {
            this.key = key;
            this.value = value;
        }

        public StringValue getKey() {
            return key;
        }

        public ObjectValue getValue() {
            return value;
        }

        public void setValue(ObjectValue value) {
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
    
    /**
     * Returns the value of the property as <code>String</code>. 
     * If the current property value is of <code>Date</code> type,
     * gets the format of date from {@link DefaultPropertyDefinitions}.
     * @param key Name of the property.
     * @return Value formatted to string. <code>Null</code> when there is
     * no property with such name.
     */
    public String getPropertyValueAsString(String key) {
        String result = null;
        Property p = getProperty(key);
        if(p != null) {
            Object propValue = p.getValue().getValue();
            if(propValue instanceof String) {
                result = (String)p.getValue().getValue();
            } else {
                if(propValue instanceof Date) {
                    String format = DefaultPropertyDefinitions.getPropertyByName(key).getFormat();  
                    if(format != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                        result = dateFormat.format((Date)propValue);
                    }
                } else {
                    if(propValue instanceof Boolean) {
                        result = ((Boolean)propValue).toString();
                    } else {
                        if(propValue instanceof Integer) {
                            result = ((Integer)propValue).toString();
                        }
                    }
                }
            }
        } 
        return result;
        //return p == null ? null : p.getValue().getValue().toString();
    }
    
    public Object getPropertyValue(String key) {
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
                property.setValue(new ObjectValue(value == null ? "" : value));
            } else {
                Property newProperty = new Property(new StringValue(name), new ObjectValue(value));
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
