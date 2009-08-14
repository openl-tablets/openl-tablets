/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.impl.DataBase;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenSchema;

/**
 * @author snshor
 *
 */
public class XlsModuleOpenClass extends ModuleOpenClass {
	
	/**
	 * Map of internal types. 
	 * XLS document can have internal types defined 
	 * using <code>Datatype</code> tables, e.g. domain model.
	 */
	private Map<String, IOpenClass> internalTypes = new HashMap<String, IOpenClass>();

    IDataBase dataBase = new DataBase();

    /**
     * @param schema
     * @param name
     */
    public XlsModuleOpenClass(IOpenSchema schema, String name, XlsMetaInfo metaInfo, OpenL openl) {
        super(schema, name, openl);
        this.metaInfo = metaInfo;
    }

    /**
     * @return
     */
    public IDataBase getDataBase() {
        return dataBase;
    }

    public XlsMetaInfo getXlsMetaInfo() {
        return (XlsMetaInfo) metaInfo;
    }
    
    /**
	 * Add new type to internal types list. If the type with the same name
	 * already exists exception will be thrown.
	 * 
	 * @param type
	 *            IOpenClass instance
	 * @throws Exception
	 *             if an error had occurred.
	 */
    @Override
	public void addType(String namespace, IOpenClass type) throws Exception {

		String typeName = buildFullTypeName(namespace, type.getName());

		if (internalTypes.containsKey(typeName)) {
			throw new Exception("The type " + typeName
					+ " has been defined already");
		}

		internalTypes.put(typeName, type);
	}

	/**
	 * Finds type with given name in internal type list. If type with given name
	 * exists in list it will be returned; <code>null</code> - otherwise.
	 * 
	 * @param typeName
	 *            name of type to search
	 * @return {@link IOpenClass} instance or <code>null</code>
	 */
    @Override
	public IOpenClass findType(String namespace, String typeName) {

		String name = buildFullTypeName(namespace, typeName);

		return internalTypes.get(name);
	}

	/**
	 * Builds full type name using namespace and type names.
	 * 
	 * @param namespace
	 *            type namespace
	 * @param type
	 *            type name
	 * @return full name string
	 */
	private String buildFullTypeName(String namespace, String type) {

		return String.format("%s.%s", namespace, type);
	}

}
