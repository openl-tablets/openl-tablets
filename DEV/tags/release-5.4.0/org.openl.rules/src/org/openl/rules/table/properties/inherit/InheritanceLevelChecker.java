package org.openl.rules.table.properties.inherit;

import java.util.Arrays;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

/**
 * Class to check properties level according to its definition.
 * 
 * @author DLiauchuk
 *
 */
public class InheritanceLevelChecker {
    
    private static final Log LOG = LogFactory.getLog(InheritanceLevelChecker.class);
    
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
                    String msg = "Property with name "+ name + " can`t be defined on the " 
                    + currentLevel.getDisplayName() + " level";
                    LOG.debug(msg);
                    throw new InvalidPropertyLevelException(msg);
                } 
            } else {
                LOG.debug("Inheritance levels were not defined for property with name "+ name);
            }
        } else {
            LOG.debug("There is no such property in Definitions with name " + name);
        }
    }
}
