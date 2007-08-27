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
public class UserContext extends AUserContext
{

  protected ClassLoader userClassLoader;

  protected String userHome;

  protected Properties userProperties;

  public UserContext(ClassLoader userClassLoader, String userHome)
  {
    this(userClassLoader, userHome, null);
  }

  public UserContext(
    ClassLoader userClassLoader,
    String userHome,
    Properties userProperties)
  {
    this.userClassLoader = userClassLoader;
    this.userHome = userHome;
    this.userProperties = userProperties;
  }

  /* (non-Javadoc)
   * @see org.openl.conf.IUserContext#getUserClassLoader()
   */
  public ClassLoader getUserClassLoader()
  {
    return userClassLoader;
  }

  /* (non-Javadoc)
   * @see org.openl.conf.IUserContext#getUserHome()
   */
  public String getUserHome()
  {
    return userHome;
  }

  /* (non-Javadoc)
   * @see org.openl.conf.IUserContext#getUserProperties()
   */
  public Properties getUserProperties()
  {
    return userProperties;
  }
  
  
  

	/**
	 *
	 */

	public String toString()
	{
		return "home=" + userHome + " cl=" + userClassLoader;
	}

}
