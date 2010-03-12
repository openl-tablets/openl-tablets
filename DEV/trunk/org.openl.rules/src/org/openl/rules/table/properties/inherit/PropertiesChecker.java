package org.openl.rules.table.properties.inherit;

import java.util.Arrays;
import java.util.Set;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.properties.TablePropertiesException;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

/**
 * Class to check properties according to some situations.
 * 
 * @author DLiauchuk
 *
 */
public class PropertiesChecker {
    
    private static final Log LOG = LogFactory.getLog(PropertiesChecker.class);
    
    /**
     * Checks if properties with given names can be defined on the current level. Checks according to the properties 
     * definitions in {@link DefaultPropertyDefinitions}. If one of the property can`t be defined on the current level
     * throws an exception. 
     * 
     * @param currentLevel current level of current properties.
     * @param propertiesNamesToCheck names of properties to check.  
     * @throws InvalidPropertyLevelException if one of property can`t be defined on current level.
     */
    public static void checkPropertiesLevel(InheritanceLevel currentLevel, Set<String> propertiesNamesToCheck) 
    throws InvalidPropertyLevelException {
        for (String propertyNameToCheck : propertiesNamesToCheck) {                        
            checkPropertyLevel(currentLevel, propertyNameToCheck);
        }
    }
    
    /**
     * Checks if property with given name can be defined on the current level. Checks according to the property 
     * definitions in {@link DefaultPropertyDefinitions}. If property can`t be defined on the current level
     * throws an exception.
     * 
     * @param currentLevel current level of current property.
     * @param name name of the property to check.
     * @throws InvalidPropertyLevelException if property can`t be defined on the current level.
     */
    public static void checkPropertyLevel(InheritanceLevel currentLevel, String name) 
    throws InvalidPropertyLevelException {
        TablePropertyDefinition propertyDefinition = TablePropertyDefinitionUtils.getPropertyByName(name);
        if (propertyDefinition != null) {
            InheritanceLevel[] inheritanceLevels = propertyDefinition.getInheritanceLevel();
            if (inheritanceLevels != null && inheritanceLevels.length > 0) {
                if (!Arrays.asList(inheritanceLevels).contains(currentLevel)) {
                    String msg = String.format("Property with name [%s] can`t be defined on the [%s] level.", name, 
                            currentLevel.getDisplayName());
                    LOG.debug(msg);
                    throw new InvalidPropertyLevelException(msg);
                } 
            } else {
                LOG.debug(String.format("Inheritance levels were not defined for property with name [%s].", name));
            }
        } else {
            LOG.debug(String.format("There is no such property in Definitions with name [%s].", name));
        }
    }
    
    /**
     * Checks if properties can be defined for given type of table.
     * 
     * @param propertyName
     * @param tableType     
     * @return TRUE if given property can be set for given type of table. 
     */
    public static boolean canSetPropertyForTableType(String propertyName, String tableType) {
        boolean result = false;
        String definitionTableType = TablePropertyDefinitionUtils.getTableTypeByPropertyName(propertyName);
        if (StringUtils.isEmpty(definitionTableType) || definitionTableType.equals(tableType)) {
            result = true;
        } else {
            // If definitionTableType is empty, it means that property is suitable for all kinds of tables.
            // If type from property definition and current table type are equals. It means property is suitable 
            // for this kind of table.
        }
        return result;
    }
}
