/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.Properties;

/**
 * @author snshor
 * IUserContext an abstraction of user environment configuration.
 *
 */
public interface IUserContext {

    public interface Executable {
        public Object execute();
    }

    public Object execute(Executable run);

    public ClassLoader getUserClassLoader();

    public String getUserHome();

    public Properties getUserProperties();

}
