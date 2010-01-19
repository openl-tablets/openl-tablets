package org.openl.rules.table.properties;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.properties.TablePropertyDefinition.InheritanceLevel;

public class InheritanceLevelChecker {
    
    private static final Log LOG = LogFactory.getLog(InheritanceLevelChecker.class);
    
    public static void checkPropertiesLevel(InheritanceLevel currentLevel, Map<String, Object> propertiesToCheck) 
    throws InvalidPropertyLevelException {
        for (Entry<String, Object> properties : propertiesToCheck.entrySet()) {
            String name = properties.getKey();            
            TablePropertyDefinition propertyDefinition = DefaultPropertyDefinitions.getPropertyByName(name);
            if (propertyDefinition != null) {
                InheritanceLevel[] inheritanceLevels = propertyDefinition.getInheritanceLevel();
                if (inheritanceLevels != null && inheritanceLevels.length > 0) {
                    if (!Arrays.asList(inheritanceLevels).contains(currentLevel)) {
                        String msg = "Property with name "+ name + " can`t be defined on the " 
                        + currentLevel.toString() + " level";
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
}
