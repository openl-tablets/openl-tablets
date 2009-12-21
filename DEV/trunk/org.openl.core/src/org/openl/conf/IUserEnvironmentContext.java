/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.Properties;

/**
 * The <code>IUserEnvironmentContext</code> defines an user environment
 * configuration abstraction.
 * 
 * @author snshor
 * 
 */
public interface IUserEnvironmentContext {

    public Object execute(IExecutable run);

    public ClassLoader getUserClassLoader();

    public String getUserHome();

    public Properties getUserProperties();

}
