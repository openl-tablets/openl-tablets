/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

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
}
