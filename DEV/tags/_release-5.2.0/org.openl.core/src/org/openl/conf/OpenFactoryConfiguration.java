/*
 * Created on Jul 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

import org.openl.OpenConfigurationException;
import org.openl.types.IOpenFactory;

/**
 * @author snshor
 *
 */
public class OpenFactoryConfiguration extends AGenericConfiguration implements IOpenFactoryConfiguration
{

	protected String name;
	protected IOpenFactory factory;

  /* (non-Javadoc)
   * @see org.openl.conf.AGenericConfiguration#getImplementingClass()
   */
  public Class<?> getImplementingClass()
  {
    return IOpenFactory.class;
  }
  
  
  public IOpenFactory getOpenFactory(IConfigurableResourceContext cxt)
  {
  	if (factory == null)
  	  factory = (IOpenFactory) createResource(cxt);
  	  
  	return factory;  
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

  /* (non-Javadoc)
   * @see org.openl.conf.IConfigurationElement#validate(org.openl.conf.IConfigurableResourceContext)
   */
  public void validate(IConfigurableResourceContext cxt)
    throws OpenConfigurationException
  {
  	if (name == null)
  	  throw new OpenConfigurationException("Factory must have a name", getUri(), null);
  	
    super.validate(cxt);
  }

}
