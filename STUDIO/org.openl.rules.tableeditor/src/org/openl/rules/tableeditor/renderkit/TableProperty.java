package org.openl.rules.tableeditor.renderkit;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

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
    private String deprecation;
    private Constraints constraints;
    private String description;
    private boolean system;
    private boolean dimensional;
    private InheritanceLevel inheritanceLevel;
    private String inheritedTableId;
    private String inheritedTableName;

    public TableProperty(TablePropertyDefinition propDefinition) {
        this.name = propDefinition.getName();
        this.displayName = propDefinition.getDisplayName();
        this.type = propDefinition.getType() == null ? String.class : propDefinition.getType().getInstanceClass();
        this.group = propDefinition.getGroup();
        this.format = propDefinition.getFormat();
        this.deprecation = propDefinition.getDeprecation();
        this.constraints = propDefinition.getConstraints();
        this.description = propDefinition.getDescription();
        this.system = propDefinition.isSystem();
        this.dimensional = propDefinition.isDimensional();
    }

    private TableProperty(TablePropertyBuilder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.value = builder.value;
        this.type = builder.type;
        this.group = builder.group;
        this.format = builder.format;
        this.deprecation = builder.deprecation;
        this.constraints = builder.constraints;
        this.description = builder.description;
        this.system = builder.system;
        this.dimensional = builder.dimensional;
        this.inheritanceLevel = builder.inheritanceLevel;
        this.inheritedTableId = builder.inheritedTableId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDeprecation() {
        return deprecation;
    }

    public void setDeprecation(String deprecation) {
        this.deprecation = deprecation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return <code>TRUE</code> if property value can be overriden on TABLE level.
     */
    public boolean isCanBeOverridenInTable() {
        return PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, name);
    }

    public boolean isFolderLevelProperty() {
        return InheritanceLevel.FOLDER.equals(inheritanceLevel);
    }

    public boolean isProjectLevelProperty() {
        return InheritanceLevel.PROJECT.equals(inheritanceLevel);
    }

    public boolean isModuleLevelProperty() {
        return InheritanceLevel.MODULE.equals(inheritanceLevel);
    }

    public boolean isCategoryLevelProperty() {
        return InheritanceLevel.CATEGORY.equals(inheritanceLevel);
    }

    public boolean isExternalPropery() {
        return InheritanceLevel.EXTERNAL.equals(inheritanceLevel);
    }

    public boolean isInheritedProperty() {
        return isModuleLevelProperty() || isCategoryLevelProperty() || isFolderLevelProperty() || isProjectLevelProperty() || isExternalPropery();
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
     * This method must be used for all the cases when you need to display property on UI. It converts its value from
     * any type to string.
     *
     * @return
     */
    public String getDisplayValue() {
        return getStringValue();
    }

    public String getStringValue() {
        String result = StringUtils.EMPTY;
        if (value != null) {
            result = FormattersManager.getFormatter(type, getFormat()).format(value);
        }
        return result;
    }

    /**
     * Setter for the property value as {@link String}. Income value will be parsed to the appropriate type. That can be
     * found calling {@link #getType()} method.
     *
     * @param value value of the property as String.
     */
    public void setStringValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            this.value = FormattersManager.getFormatter(type, getFormat()).parse(value);
        } else {
            this.value = null;
        }
    }

    public String[] getEnumArrayValue() {
        if (value != null && isEnumArray()) {
            return EnumUtils.getNames((Enum<?>[]) value);
        }
        return null;
    }

    public void setEnumArrayValue(String[] value) {
        Object[] resultArray = (Object[]) Array.newInstance(type.getComponentType(), value.length);

        for (int i = 0; i < value.length; i++) {
            resultArray[i] = EnumUtils.valueOf(type.getComponentType(), value[i]);
        }

        this.value = resultArray;
    }

    public List<SelectItem> getEnumArrayItems() {
        List<SelectItem> items = new ArrayList<>();
        String[] values = null;
        String[] displayValues = null;

        if (isEnumType() || isEnumArray()) {
            Class<?> instanceClass = type.getComponentType() != null ? type.getComponentType() : type;

            values = EnumUtils.getNames(instanceClass);
            displayValues = EnumUtils.getValues(instanceClass);
        }

        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                items.add(new SelectItem(values[i], displayValues[i]));
            }
        }

        return items;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    /**
     * This is a setter for the value of the property. Value must be always typify. This method is commonly used from
     * UI. If property <code>{@link #isDateType()}</code>, <code>{@link #isBooleanType()}</code> UI controls will be
     * typify, and the income value will be of the appropriate type. And if the income value is String we try to parse
     * it to the appropriate type.
     *
     * @param value a value of the property.
     */
    public void setValue(Object value) {
        if (value instanceof String) {
            String valueStr = (String) value;
            if (StringUtils.isNotBlank(valueStr)) {
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

    public boolean isDimensional() {
        return dimensional;
    }

    public void setDimensional(boolean dimensional) {
        this.dimensional = dimensional;
    }

    public InheritanceLevel getInheritanceLevel() {
        return inheritanceLevel;
    }

    public void setInheritanceLevel(InheritanceLevel inheritanceLevel) {
        this.inheritanceLevel = inheritanceLevel;
    }

    public String getInheritedTableId() {
        return inheritedTableId;
    }

    public void setInheritedTableId(String inheritedTableId) {
        this.inheritedTableId = inheritedTableId;
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
        private String deprecation;
        private Constraints constraints;
        private String description;
        private boolean system;
        private boolean dimensional;
        private InheritanceLevel inheritanceLevel;
        private String inheritedTableId;

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

        public TablePropertyBuilder deprecation(String val) {
            deprecation = val;
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

        public TablePropertyBuilder dimensional(boolean val) {
            dimensional = val;
            return this;
        }

        public TablePropertyBuilder inheritanceLevel(InheritanceLevel val) {
            inheritanceLevel = val;
            return this;
        }

        public TablePropertyBuilder inheritedTableId(String val) {
            inheritedTableId = val;
            return this;
        }

        public TableProperty build() {
            return new TableProperty(this);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append(getDisplayName()).append(" : ").append(getDisplayValue()).toString();
    }

    public String getInheritedTableName() {
        return inheritedTableName;
    }

    public void setInheritedTableName(String inheritedTableName) {
        this.inheritedTableName = inheritedTableName;
    }

}
