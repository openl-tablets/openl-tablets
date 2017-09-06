/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

/**
 * The class is designed as immutable, but not immutable because contains
 * ClassLoader.
 * 
 * @author snshor
 * 
 */
public final class UserContext extends AUserContext {

    private ClassLoader userClassLoader;

    private String userHome;

    private Properties userProperties;

    public UserContext(ClassLoader userClassLoader, String userHome) {
        this(userClassLoader, userHome, null);
    }

    public UserContext(ClassLoader userClassLoader, String userHome, Properties userProperties) {
        this.userClassLoader = userClassLoader;

        this.userHome = new File(userHome).getAbsolutePath();
        this.userProperties = userProperties;
    }

    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    public String getUserHome() {
        return userHome;
    }

    public Properties getUserProperties() {
        return new Properties(userProperties);
    }

    private String printClassloader(ClassLoader ucl) {
        if (ucl == null) {
            return "null";
        }
        if (ucl instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) ucl).getURLs();
            StringBuilder sb = new StringBuilder();
            sb.append("ClassLoader URLs: ");
            for (URL url : urls) {
                sb.append(url.toExternalForm());
                sb.append(',');
            }
            return sb.toString();
        }
        return ucl.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("home=").append(userHome).append("cl=").append(printClassloader(userClassLoader));
        return sb.toString();
    }

}
