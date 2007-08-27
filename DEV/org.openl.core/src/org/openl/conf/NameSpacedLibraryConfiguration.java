/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

import org.openl.OpenConfigurationException;
import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.MethodSearch;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.CollectionsUtil;

/**
 * @author snshor
 *
 */
public class NameSpacedLibraryConfiguration extends AConfigurationElement
{

	String namespace;

	IMethodFactoryConfigurationElement[] factories = {};	

  /* (non-Javadoc)
   * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
   */
  public void validate(IConfigurableResourceContext cxt)
    throws OpenConfigurationException
  {
		for (int i = 0; i < factories.length; i++)
    {
      factories[i].validate(cxt);
    }
  }

  /**
   * @return
   */
  public String getNamespace()
  {
    return namespace;
  }

  /**
   * @param string
   */
  public void setNamespace(String string)
  {
    namespace = string;
  }

	public IOpenField getField(
		String name,
	IConfigurableResourceContext cxt)
    
	{
		for (int i = 0; i < factories.length; i++)
		{
			IOpenField field = factories[i].getLibrary(cxt).getVar(name);
			if (field != null)
				return field;
		}
    
		return null;
	}



  /* (non-Javadoc)
   * @see org.openl.binding.IMethodFactory#getMethodCaller(java.lang.String, org.openl.types.IOpenClass[], org.openl.binding.ICastFactory)
   */
  public IMethodCaller getMethodCaller(
    String name,
    IOpenClass[] params,
    ICastFactory casts,
	IConfigurableResourceContext cxt)
    
    throws AmbiguousMethodException
  {
  	for (int i = 0; i < factories.length; i++)
    {
      IMethodCaller mc =  MethodSearch.getMethodCaller(name, params, casts, factories[i].getLibrary(cxt));
      if (mc != null)
        return mc;
    }
    
    return null;
  }
  
  public void addJavalib(JavaLibraryConfiguration factory)
  {
  	factories = (IMethodFactoryConfigurationElement[])CollectionsUtil.add(factories, factory);
  }
  
  
  public void addAnyLibrary(GenericLibraryConfiguration glb)
  {
		factories = (IMethodFactoryConfigurationElement[])CollectionsUtil.add(factories, glb);
  }

}
