package org.openl.rules.table.properties;

public class DefaultTablePropertiesHolder implements ITablePropertiesHolder {
	
	private ITablePropertiesHolder parent;
	private String propertiesLevel;
	private ITableProperties tableProperties;
	
	public DefaultTablePropertiesHolder(String propertiesLevel, ITableProperties tableProperties) {
		this(null, propertiesLevel, tableProperties);
	}
	
	public DefaultTablePropertiesHolder(ITablePropertiesHolder parent, String propertiesLevel,
	        ITableProperties tableProperties) {
		this.parent = parent;
		this.propertiesLevel = propertiesLevel;
		this.tableProperties = tableProperties;
	}
	
	public ITablePropertiesHolder getParent() {
		return parent;
	}
	
	public String getPropertiesLevel() {
		return propertiesLevel;
	}
	
	public ITableProperties getTableProperties() {
		return tableProperties;
	}
	
	public boolean isDefined(String propertyName) {
		return tableProperties.isDefined(propertyName);
	}
}
