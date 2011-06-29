package org.openl.rules.tableeditor.renderkit;

import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;

/**
 * Temporary class for holding table properties
 * 
 * @author DLiauchuk
 * 
 */
public class TableProperty {
    private String name;
    private String displayName;
    private Object value;
    private Class<?> type;

    private String group;
    private String format;
    private Constraints constraints;
    private String description;
    private boolean system;
    private InheritanceLevel inheritanceLevel;

    private TableProperty(TablePropertyBuilder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.value = builder.value;
        this.type = builder.type;
        this.group = builder.group;
        this.format = builder.format;
        this.constraints = builder.constraints;
        this.description = builder.description;
        this.system = builder.system;
        this.inheritanceLevel = builder.inheritanceLevel;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return <code>TRUE</code> if property value can be overriden on TABLE
     *         level.
     */
    public boolean canBeOverridenInTable() {
        boolean result = PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, name);        
        return result;
    }

    public boolean isModuleLevelProperty() {
        return InheritanceLevel.MODULE.equals(inheritanceLevel);
    }

    public boolean isCategoryLevelProperty() {
        return InheritanceLevel.CATEGORY.equals(inheritanceLevel);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Object getValue() {
        return value;
    }

    /**
     * This method must be used for all the cases when you need to display
     * property on UI. It converts its value from any type to string.
     * 
     * @return
     */
    public String getDisplayValue() {
        return StringEscapeUtils.escapeHtml(
                getStringValue());
    }

    /**
     * Setter for the property value as {@link String}. Income value will be parsed to the appropriate type.
     * That can be found calling {@link #getType()} method.
     * 
     * @param value value of the property as String.
     */
    public void setStringValue(String value) {
        Object result = value;
        if (StringUtils.isNotEmpty(value)) {
            result = FormattersManager.getFormatter(type, getFormat()).parse(value);            
        } else {
            result = null;
        }
        this.value = result;
    }

    public String getStringValue() {        
        String result = StringUtils.EMPTY;       
        if (value != null) {
            result = FormattersManager.getFormatter(type, getFormat()).format(value); 
        }        
        return result;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
    
    /**
     * This is a setter for the value of the property. Value must be always typify. 
     * This method is commonly used from UI. If property <code>{@link #isDateType()}</code>, 
     * <code>{@link #isBooleanType()}</code> UI controls will be typify, and the income 
     * value will be of the appropriate type. And if the income value is String we try 
     * to parse it to the appropriate type.
     *  
     * @param value a value of the property. 
     */
    public void setValue(Object value) {         
        if (value instanceof String) {
            String valueStr = (String)value;
            if (StringUtils.isNotEmpty(valueStr)) {
                value = FormattersManager.getFormatter(type, getFormat()).parse(valueStr);
            } else {
                value = null;
            }
        }
        this.value = value;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isStringType() {
        return String.class.equals(type);
    }

    public boolean isDateType() {
        return Date.class.equals(type);
    }

    public boolean isBooleanType() {
        return Boolean.class.equals(type);
    }

    public boolean isDoubleType() {
        return Double.class.equals(type);
    }

    public boolean isEnumType() {
        return type != null && type.isEnum();
    }

    public boolean isEnumArray() {
        return type != null && type.isArray() && type.getComponentType().isEnum();
    }
    
    /**     
     * Checks if the current type is <code>String[]</code>
     * 
     * @return true if type is <code>String[]</code>     
     */
    public boolean isStringArray() {
        return type != null && type.isArray() && String.class.equals(type.getComponentType());
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isSystem() {
        return system;
    }

    public InheritanceLevel getInheritanceLevel() {
        return inheritanceLevel;
    }

    public void setInheritanceLevel(InheritanceLevel inheritanceLevel) {
        this.inheritanceLevel = inheritanceLevel;
    }

    /**
     * Builder for TableProperties
     * 
     * @author DLiauchuk
     * 
     */
    public static class TablePropertyBuilder {
        // Required parameters
        private String name;        
        private Class<?> type;
        
        // Optional parameters
        private String displayName;
        private Object value;        
        private String group;
        private String format;
        private Constraints constraints;
        private String description;
        private boolean system;
        private InheritanceLevel inheritanceLevel;

        public TablePropertyBuilder(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public TablePropertyBuilder value(Object val) {
            value = val;
            return this;
        }

        public TablePropertyBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public TablePropertyBuilder group(String val) {
            group = val;
            return this;
        }

        public TablePropertyBuilder format(String val) {
            format = val;
            return this;
        }

        public TablePropertyBuilder constraints(Constraints val) {
            constraints = val;
            return this;
        }

        public TablePropertyBuilder description(String val) {
            description = val;
            return this;
        }

        public TablePropertyBuilder system(boolean val) {
            system = val;
            return this;
        }

        public TablePropertyBuilder inheritanceLevel(InheritanceLevel val) {
            inheritanceLevel = val;
            return this;
        }

        public TableProperty build() {
            return new TableProperty(this);
        }
    }
}
