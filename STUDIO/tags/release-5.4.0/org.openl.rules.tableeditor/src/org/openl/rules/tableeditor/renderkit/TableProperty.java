package org.openl.rules.tableeditor.renderkit;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.InheritanceLevelChecker;
import org.openl.rules.table.properties.inherit.InvalidPropertyLevelException;
import org.openl.util.ArrayUtils;
import org.openl.util.EnumUtils;

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
        boolean result = false;
        try {
            InheritanceLevelChecker.checkPropertyLevel(InheritanceLevel.TABLE, name);
            result = true;
        } catch (InvalidPropertyLevelException e) {
        }
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
        return getStringValue();
    }

    public void setStringValue(String value) {
        // TODO Refactor with property type
        this.value = value;
    }

    public String getStringValue() {
        
        String result = "";
       
        if (value != null) {
        
            if (value instanceof Date) {
                SimpleDateFormat sdf = new SimpleDateFormat(getFormat());
                result = sdf.format((Date) value);
            
            } else if (EnumUtils.isEnum(value)) {
                result = ((Enum<?>) value).name();
            
            } else if (EnumUtils.isEnumArray(value)) {
            
                Object[] enums = (Object[]) value;
                
                if (!ArrayUtils.isEmpty(enums)) {
                    String[] names = EnumUtils.getNames(enums);
                    result = StringUtils.join(names, ",");
                }
            } else if (isSimpleArray()) {
                if (StringUtils.isNotEmpty((String)value)) {
                    Object[] array = (Object[]) value;
                    result = StringUtils.join(array, ",");
                }

            } else {
                result = value.toString();
            }
        }
        
        return result;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isString() {
        return String.class.equals(type);
    }

    public boolean isDate() {
        return Date.class.equals(type);
    }

    public boolean isBoolean() {
        return Boolean.class.equals(type);
    }

    public boolean isDouble() {
        return Double.class.equals(type);
    }

    public boolean isEnum() {
        return type != null && type.isEnum();
    }

    public boolean isEnumArray() {
        return type != null && type.isArray() && type.getComponentType().isEnum();
    }
    
    public boolean isSimpleArray() {
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
        private String displayName;

        // Optional parameters
        private Object value;
        private Class<?> type;
        private String group;
        private String format;
        private Constraints constraints;
        private String description;
        private boolean system;
        private InheritanceLevel inheritanceLevel;

        public TablePropertyBuilder(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        public TablePropertyBuilder value(Object val) {
            value = val;
            return this;
        }

        public TablePropertyBuilder type(Class<?> val) {
            type = val;
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
