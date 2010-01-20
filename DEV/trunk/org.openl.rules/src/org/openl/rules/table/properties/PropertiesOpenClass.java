package org.openl.rules.table.properties;

import org.openl.rules.table.properties.def.TablePropertyDefinition;

/**
 * 
 * @author snshor
 * Created Jul 21, 2009 
 *
 *	This class is used to load TableProperties as data beans
 */
//TODO: Unused class. Should be deleted.
@Deprecated
public class PropertiesOpenClass 
{
	
	Class<?> propertiesBeanClass;
	TablePropertyDefinition[] definitions;
	
	public PropertiesOpenClass(Class<?> propertiesBeanClass, TablePropertyDefinition[] definitions)
	{
		this.propertiesBeanClass = propertiesBeanClass;
		this.definitions = definitions;
		initialize();
	}

	private void initialize() {
		// TODO Auto-generated method stub
		
	}
}
