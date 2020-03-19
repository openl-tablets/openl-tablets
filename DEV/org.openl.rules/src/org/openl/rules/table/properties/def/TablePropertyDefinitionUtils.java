package org.openl.rules.table.properties.def;

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;

/**
 * Helper methods, for working with properties.<br>
 * See also {@link PropertiesChecker} for more methods.
 *
 * @author DLiauchuk
 *
 */
public final class TablePropertyDefinitionUtils {

    private TablePropertyDefinitionUtils() {
    }

    private static final List<TablePropertyDefinition> PROPERTIES_TO_BE_SET_BY_DEFAULT;
    private static final Map<String, Object> PROPERTIES_MAP_TO_BE_SET_BY_DEFAULT;

    static {
        List<TablePropertyDefinition> propertiesToBeSetByDefault = new ArrayList<>();
        for (TablePropertyDefinition propDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (propDefinition.getDefaultValue() != null) {
                propertiesToBeSetByDefault.add(propDefinition);
            }
        }
        PROPERTIES_TO_BE_SET_BY_DEFAULT = Collections.unmodifiableList(propertiesToBeSetByDefault);

        Map<String, Object> propertiesMapToBeSetByDefault = new HashMap<>();

        for (TablePropertyDefinition propertyWithDefaultValue : propertiesToBeSetByDefault) {
            String defaultPropertyName = propertyWithDefaultValue.getName();
            TablePropertyDefinition propertyDefinition = TablePropertyDefinitionUtils
                .getPropertyByName(defaultPropertyName);
            Class<?> defaultPropertyValueType = propertyDefinition.getType().getInstanceClass();

            IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(defaultPropertyValueType);
            Object defaultValue = converter.parse(propertyWithDefaultValue.getDefaultValue(),
                propertyWithDefaultValue.getFormat());

            propertiesMapToBeSetByDefault.put(defaultPropertyName, defaultValue);
        }

        PROPERTIES_MAP_TO_BE_SET_BY_DEFAULT = Collections.unmodifiableMap(propertiesMapToBeSetByDefault);
    }

    private static String[] dimensionalTablePropertiesNames = null;

    /**
     * Gets the array of properties names that are dimensional.
     *
     * @return names of properties that are dimensional.
     */
    public static String[] getDimensionalTablePropertiesNames() {
        if (dimensionalTablePropertiesNames == null) {
            List<String> names = new ArrayList<>();
            List<TablePropertyDefinition> dimensionalProperties = getDimensionalTableProperties();

            for (TablePropertyDefinition definition : dimensionalProperties) {
                names.add(definition.getName());
            }

            dimensionalTablePropertiesNames = names.toArray(new String[names.size()]);
        }
        return dimensionalTablePropertiesNames;
    }

    private static List<TablePropertyDefinition> dimensionalTableProperties = null;

    /**
     * Gets the array of properties names that are dimensional.
     *
     * @return names of properties that are dimensional.
     */
    public static List<TablePropertyDefinition> getDimensionalTableProperties() {
        if (dimensionalTableProperties == null) {
            List<TablePropertyDefinition> dimensionalProperties = new ArrayList<>();
            TablePropertyDefinition[] definitions = DefaultPropertyDefinitions.getDefaultDefinitions();

            for (TablePropertyDefinition definition : definitions) {
                if (definition.isDimensional()) {
                    dimensionalProperties.add(definition);
                }
            }

            dimensionalTableProperties = Collections.unmodifiableList(dimensionalProperties);
        }
        return dimensionalTableProperties;
    }

    /**
     * Gets the name of the property by the given display name
     *
     * @param displayName
     * @return name
     */
    public static String getPropertyName(String displayName) {
        for (TablePropertyDefinition propDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (propDefinition.getDisplayName().equals(displayName)) {
                return propDefinition.getName();
            }
        }
        return null;
    }

    /**
     * Gets the display name of the property by the given name
     *
     * @param name
     * @return diplayName
     */
    public static String getPropertyDisplayName(String name) {
        for (TablePropertyDefinition propDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (propDefinition.getName().equals(name)) {
                return propDefinition.getDisplayName();
            }
        }
        return null;
    }

    /**
     * Gets the property by its given name
     *
     * @param name
     * @return property definition
     */
    public static TablePropertyDefinition getPropertyByName(String name) {
        for (TablePropertyDefinition propDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (propDefinition.getName().equals(name)) {
                return propDefinition;
            }
        }
        return null;
    }

    public static Class<?> getTypeByPropertyName(String propertyName) {
        TablePropertyDefinition tablePropertyDefinition = TablePropertyDefinitionUtils.getPropertyByName(propertyName);
        if (tablePropertyDefinition != null) {
            return tablePropertyDefinition.getType().getInstanceClass();
        }
        return null;
    }

    public static boolean isPropertyExist(String propertyName) {
        return getPropertyByName(propertyName) != null;
    }

    /**
     * Gets list of properties that must me set for every table by default.
     *
     * @return list of properties.
     */
    public static List<TablePropertyDefinition> getPropertiesToBeSetByDefault() {
        return PROPERTIES_TO_BE_SET_BY_DEFAULT;
    }

    /**
     * Gets map of properties that must me set for every table by default.
     *
     * @return list of properties.
     */
    public static Map<String, Object> getPropertiesMapToBeSetByDefault() {
        return PROPERTIES_MAP_TO_BE_SET_BY_DEFAULT;
    }

    /**
     * Gets list of properties that are marked as system.
     *
     * @return list of properties.
     */
    public static List<TablePropertyDefinition> getSystemProperties() {
        List<TablePropertyDefinition> result = new ArrayList<>();
        for (TablePropertyDefinition propDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (propDefinition.isSystem()) {
                result.add(propDefinition);
            }
        }
        return result;
    }

    public static TablePropertyDefinition[] getDefaultDefinitionsByInheritanceLevel(InheritanceLevel inheritanceLevel) {
        List<TablePropertyDefinition> resultDefinitions = new ArrayList<>();
        for (TablePropertyDefinition propertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            if (ArrayUtils.contains(propertyDefinition.getInheritanceLevel(), inheritanceLevel)) {
                resultDefinitions.add(propertyDefinition);
            }
        }
        return resultDefinitions.toArray(new TablePropertyDefinition[0]);
    }

    public static TablePropertyDefinition[] getDefaultDefinitionsForTable(String tableType) {
        return getDefaultDefinitionsForTable(tableType, null, false);
    }

    public static TablePropertyDefinition[] getDefaultDefinitionsForTable(String tableType,
            InheritanceLevel inheritanceLevel,
            boolean ignoreSystem) {
        List<TablePropertyDefinition> resultDefinitions = new ArrayList<>();

        for (TablePropertyDefinition propertyDefinition : DefaultPropertyDefinitions.getDefaultDefinitions()) {
            String name = propertyDefinition.getName();
            if (PropertiesChecker.isPropertySuitableForTableType(name, tableType) && (inheritanceLevel == null // any
                    // level
                    || ArrayUtils.contains(propertyDefinition.getInheritanceLevel(),
                        inheritanceLevel)) && (!ignoreSystem || !propertyDefinition.isSystem())) {
                resultDefinitions.add(propertyDefinition);
            }
        }

        return resultDefinitions.toArray(new TablePropertyDefinition[resultDefinitions.size()]);
    }

    public static Map<String, List<TablePropertyDefinition>> groupProperties(TablePropertyDefinition[] properties) {
        Map<String, List<TablePropertyDefinition>> groups = new LinkedHashMap<>();

        for (TablePropertyDefinition property : properties) {
            String groupName = property.getGroup();
            List<TablePropertyDefinition> group = groups.computeIfAbsent(groupName, e -> new ArrayList<>());
            group.add(property);
        }

        return groups;
    }

    /**
     * Gets the table types in which this property can be defined.
     *
     * @param propertyName property name.
     * @return the table type in which this property can be defined. <code>NULL</code> if property can be defined for
     *         each type of tables.
     */
    public static XlsNodeTypes[] getSuitableTableTypes(String propertyName) {
        TablePropertyDefinition propDefinition = getPropertyByName(propertyName);
        if (propDefinition != null) {
            return propDefinition.getTableType();
        }
        return null;
    }

    public static Class<?> getPropertyTypeByPropertyName(String name) {
        TablePropertyDefinition propDefinition = getPropertyByName(name);
        if (propDefinition != null) {
            return propDefinition.getType().getInstanceClass();
        }
        return null;
    }
}
