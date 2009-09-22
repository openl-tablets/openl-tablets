package org.openl.rules.context.properties;

public class DefaultContextPropertyDefinitions {
	
	public static ContextPropertyDefinition[] getDefaultDefinitions() {
		
		ContextPropertyDefinition[] definitions = null;
		// <<< INSERT >>>
		definitions = new ContextPropertyDefinition[1];
		definitions[0] = new ContextPropertyDefinition();
		definitions[0].setDescription("Date");
		definitions[0].setName("currentDate");
		definitions[0].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		// <<< END INSERT >>>
		
		return definitions;
	}
}
