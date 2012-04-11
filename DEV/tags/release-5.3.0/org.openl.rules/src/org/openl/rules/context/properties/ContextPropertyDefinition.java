package org.openl.rules.context.properties;

import org.openl.types.IOpenClass;

public class ContextPropertyDefinition {
	
	private String name;
	
	private IOpenClass type;
	
	private String description;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public IOpenClass getType() {
		return type;
	}
	
	public void setType(IOpenClass type) {
		this.type = type;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
}
