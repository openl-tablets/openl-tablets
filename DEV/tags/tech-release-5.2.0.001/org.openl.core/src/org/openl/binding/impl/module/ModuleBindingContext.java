/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.AmbiguousVarException;
import org.openl.binding.DuplicatedVarException;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.MethodSearch;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 * 
 */
public class ModuleBindingContext extends BindingContextDelegator
{

    DeferredMethod[] method;

    ModuleOpenClass module;

    /**
     * @param delegate
     */
    public ModuleBindingContext(IBindingContext delegate, ModuleOpenClass module)
    {
	super(delegate);
	this.module = module;
    }

//    public void addMethod(DeferredMethod dm)
//    {
//	module.addMethod(dm);
//    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IBindingContext#findMethodCaller(java.lang.String,
     *      java.lang.String, org.openl.types.IOpenClass[])
     */
    public IMethodCaller findMethodCaller(String namespace, String methodName,
	    IOpenClass[] parTypes) throws AmbiguousMethodException
    {

	IMethodCaller imc = null;
	if (ISyntaxConstants.THIS_NAMESPACE.equals(namespace))
	{
	    imc = MethodSearch.getMethodCaller(methodName, parTypes, this,
		    module);
	}

	return imc != null ? imc : super.findMethodCaller(namespace,
		methodName, parTypes);
    }

    /**
     * @return
     */
    public ModuleOpenClass getModule()
    {
	return module;
    }

    public ILocalVar addVar(String namespace, String name, IOpenClass type)
	    throws DuplicatedVarException
    {
	return null;
    }

    public IOpenField findVar(String namespace, String name, boolean strictMatch)
	    throws AmbiguousVarException
    {
	IOpenField res = null;
	if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE))
	    res = module.getField(name, strictMatch);

	return res != null ? res : super.findVar(namespace, name, strictMatch);
    }

    /**
     * 
     */

    public synchronized void addType(String namespace, IOpenClass type) throws Exception
    {
	String key = typeKey(namespace, type.getName());
	Map<String, IOpenClass> map = internalTypes();
	if (map.containsKey(key))
	    throw new Exception("Type " + key + " has been defined already");

	map.put(key, type);
    }

    final String typeKey(String namespace, String typeName)
    {
	return namespace + "::" + typeName;
    }

    synchronized Map<String, IOpenClass> internalTypes()
    {
	if (internalTypes == null)
	    internalTypes = new HashMap<String, IOpenClass>();
	return internalTypes;
    }

    Map<String, IOpenClass> internalTypes = null;

    /**
     * 
     */

    public IOpenClass findType(String namespace, String typeName)
    {

	if (internalTypes != null)
	{
	    String key = typeKey(namespace, typeName);
	    IOpenClass ioc = internalTypes.get(key);
	    if (ioc != null)
		return ioc;
	}

	return super.findType(namespace, typeName);
    }

}
