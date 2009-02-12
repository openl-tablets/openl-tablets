/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.OpenConfigurationException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenSchema;
import org.openl.types.ITypeLibrary;
import org.openl.types.impl.DynamicTypeLibrary;

/**
 * @author snshor
 *
 */
public class DynamicTypesConfiguration extends AConfigurationElement implements ITypeFactoryConfigurationElement
{
	
	
	DynamicTypeLibrary library = null;


	List<DynamicTypeConfiguration> dynamicTypes = new ArrayList<DynamicTypeConfiguration>();


  /* (non-Javadoc)
   * @see org.openl.newconf.IMethodFactoryConfigurationElement#getFactory()
   */
  public synchronized ITypeLibrary getLibrary(IConfigurableResourceContext cxt)
  {
  	if (library == null)
  	{
  		library = new DynamicTypeLibrary();
			for (Iterator<DynamicTypeConfiguration> iter = dynamicTypes.iterator(); iter.hasNext();)
			{
				DynamicTypeConfiguration dtc = (DynamicTypeConfiguration)iter.next();
				
				library.addType(dtc.getName(), dtc.makeOpenClass(cxt));	
			}
  		
  	}
    return library;
  }

  /* (non-Javadoc)
   * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
   */
  public void validate(IConfigurableResourceContext cxt)
    throws OpenConfigurationException
  {
  	for (Iterator<DynamicTypeConfiguration> iter = dynamicTypes.iterator(); iter.hasNext();)
    {
      DynamicTypeConfiguration dtc = (DynamicTypeConfiguration)iter.next();
      
      dtc.validate(cxt);
    }
  }
  
  public void addType(DynamicTypeConfiguration dtc)
  {
  	dynamicTypes.add(dtc);
  }


	
	static public class DynamicTypeConfiguration extends AConfigurationElement
	{
		
		String name;
		String className;
		
	  /* (non-Javadoc)
     * @see org.openl.conf.AGenericConfiguration#getImplementingClass()
     */
    public Class<?> getImplementingClass()
    {
      return IOpenClass.class;
    }
    

    /**
     * @return
     */
    public String getName()
    {
      return name;
    }

    /**
     * @param string
     */
    public void setName(String string)
    {
      name = string;
    }

    /**
     * @return
     */
    public String getClassName()
    {
      return className;
    }

    /**
     * @param string
     */
    public void setClassName(String string)
    {
      className = string;
    }

    /* (non-Javadoc)
     * @see org.openl.conf.IConfigurationElement#validate(org.openl.conf.IConfigurableResourceContext)
     */
    public void validate(IConfigurableResourceContext cxt)
      throws OpenConfigurationException
    {
    	
    	if (name == null)
    	  throw new OpenConfigurationException("Attribute name mudt not be empty", getUri(), null);
    	  
			Class<?> c = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());
			
			Class<?>[] params = {IOpenSchema.class, String.class};
			ClassFactory.validateHasConstructor(c, params, getUri());
    }
    
    IOpenClass makeOpenClass(IConfigurableResourceContext cxt)
    {
      try
      {
        Class<?> c = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());
        
        Class<?>[] paramTypes = {IOpenSchema.class, String.class};
        Constructor<?> cc = ClassFactory.validateHasConstructor(c, paramTypes, getUri());
        
        
        Object[] params = {null, name};
        return  (IOpenClass)cc.newInstance(params);
      }
      catch (Throwable e)
      {
        throw new OpenConfigurationException("Can not create OpenClass: " + name, getUri(), e);
      }
    }

}
	
	
	


}
