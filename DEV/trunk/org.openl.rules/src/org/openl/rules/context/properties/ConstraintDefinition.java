package org.openl.rules.context.properties;

import org.openl.rules.table.properties.TablePropertyDefinition;

public class ConstraintDefinition {
	
	private TablePropertyDefinition tablePropertyDefinition;
	private IMatcher matcher;

	public ConstraintDefinition(TablePropertyDefinition tablePropertyDefinition, IMatcher matcher) {

		this.tablePropertyDefinition = tablePropertyDefinition;
		this.matcher = matcher;
    }

	public TablePropertyDefinition getTablePropertyDefinition() {
    	return tablePropertyDefinition;
    }

	public IMatcher getMatcher() {
    	return matcher;
    }
}
