/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.openl.OpenL;

/**
 * The <code>IUserContext</code> defines an user environment configuration
 * abstraction.
 * 
 * @author snshor
 * 
 */
public interface IUserContext {

    ClassLoader getUserClassLoader();

    String getUserHome();

    public IOpenLConfiguration getOpenLConfiguration(String name) throws OpenConfigurationException;

    public void registerOpenLConfiguration(String name, IOpenLConfiguration oplc) throws OpenConfigurationException;

    public OpenL getOpenL(String name);

    public void registerOpenL(String name, OpenL opl) throws OpenConfigurationException;

}
