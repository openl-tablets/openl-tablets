/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.conf;

import java.util.Properties;

/**
 * @author snshor
 *
 */
public interface IUserContext
{

  public ClassLoader getUserClassLoader();
  
  public String getUserHome();

  public Properties getUserProperties();
}
