package org.openl.rules.context.properties;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TablePropertyDefinition;

public class DefaultConstraintDefinitions {
	
	public static ConstraintDefinition[] getConstraintDefinitions() {
		
		TablePropertyDefinition[] tablePropertyDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
		
		ConstraintDefinition[] definitions = null;
		
		// <<< INSERT >>>
		definitions = new ConstraintDefinition[2];
		definitions[0] = new ConstraintDefinition(tablePropertyDefinitions[4], new IMatcher() {
			public boolean isMatch(IRulesContext context, ITableProperties tableProperties) {
				java.util.Date effectiveDate = tableProperties.getEffectiveDate();
				if (effectiveDate == null) {
					return true;
				}
				return tableProperties.getEffectiveDate().compareTo(context.getCurrentDate()) <= 0;
			}
		});
		definitions[1] = new ConstraintDefinition(tablePropertyDefinitions[5], new IMatcher() {
			public boolean isMatch(IRulesContext context, ITableProperties tableProperties) {
				java.util.Date expirationDate = tableProperties.getExpirationDate();
				if (expirationDate == null) {
					return true;
				}
				return tableProperties.getExpirationDate().compareTo(context.getCurrentDate()) >= 0;
			}
		}); // <<< END INSERT >>>
		
		return definitions;
	}
}
