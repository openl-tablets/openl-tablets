/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl;

//import org.apache.commons.lang.exception.NestableException;
import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * @author snshor
 *
 */

public class OpenConfigurationException extends NestableRuntimeException
{
  //TODO add parameters, message etc.
  
  /**
	 * 
	 */
	private static final long serialVersionUID = 3292629986027365336L;
	String uri;
  Throwable cause;
  
	public OpenConfigurationException(String msg, @SuppressWarnings("unused")
	String uri, Throwable t)
	{
		super(msg, t);
	}
	
  /**
   * @return
   */
  public String getUri()
  {
    return uri;
  }
  
  

  

}
