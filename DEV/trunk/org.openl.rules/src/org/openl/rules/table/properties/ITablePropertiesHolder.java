package org.openl.rules.table.properties;

//TODO: Unused. Should be deleted.
@Deprecated
public interface ITablePropertiesHolder {
	
	ITableProperties getTableProperties();
	ITablePropertiesHolder getParent();
	//boolean isDefined(String propertyName);
	String getPropertiesLevel();
}
