/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.Properties;

/**
 * The <code>IUserContext</code> defines an user environment
 * configuration abstraction.
 * 
 * @author snshor
 * 
 */
public interface IUserContext {

    ClassLoader getUserClassLoader();

    String getUserHome();

    Properties getUserProperties();

}
