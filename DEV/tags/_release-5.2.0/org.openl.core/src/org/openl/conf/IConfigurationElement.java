/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

import org.openl.OpenConfigurationException;

/**
 * @author snshor
 *
 */
public interface IConfigurationElement
{
	/**
	 * Checks that configuration is valid, for example that class exists and conforms to the interface
	 * @throws OpenConfigurationException
	 */
	
	void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException;

//	Object getResource(IConfigurationContext cxt) throws OpenConfigurationException; 
	
	/**
	 * 
	 * @return URI of this element (if known) or null
	 */
	String getUri();
}
