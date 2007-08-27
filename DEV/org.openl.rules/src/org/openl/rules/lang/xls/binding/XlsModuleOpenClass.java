/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls.binding;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.impl.DataBase;
import org.openl.types.IOpenSchema;


/**
 * @author snshor
 *
 */
public class XlsModuleOpenClass extends ModuleOpenClass
{
	
	IDataBase dataBase = new DataBase();
	OpenL openl;
	
	

	/**
	 * @param schema
	 * @param name
	 */
	public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl)
	{
		super(schema, name);
		this.metaInfo = metaInfo;
		this.openl = openl;
	}

	/**
	 * @return
	 */
	public IDataBase getDataBase()
	{
		return dataBase;
	}

	public OpenL getOpenl()
	{
		return this.openl;
	}
	
	public XlsMetaInfo getXlsMetaInfo()
	{
		return (XlsMetaInfo)metaInfo;
	}

}
