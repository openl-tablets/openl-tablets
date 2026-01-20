package org.openl.conf;

/**
 * The <code>IUserContext</code> defines an user environment configuration abstraction.
 *
 * @author snshor
 */
public interface IUserContext {

    ClassLoader getUserClassLoader();

}
