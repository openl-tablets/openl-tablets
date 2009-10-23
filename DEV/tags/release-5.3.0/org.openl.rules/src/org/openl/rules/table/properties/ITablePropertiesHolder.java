package org.openl.rules.table.properties;

public interface ITablePropertiesHolder {
	
	ITableProperties getTableProperties();
	ITablePropertiesHolder getParent();
	boolean isDefined(String propertyName);
	String getPropertiesLevel();
}
