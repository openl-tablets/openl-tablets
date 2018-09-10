/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;

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

    public UserContext(ClassLoader userClassLoader, String userHome) {
        this.userClassLoader = userClassLoader;
        this.userHome = new File(userHome).getAbsolutePath();
    }

    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    public String getUserHome() {
        return userHome;
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

    private Map<String, IOpenLConfiguration> configurations = new HashMap<String, IOpenLConfiguration>();

    private Map<String, OpenL> openls = new HashMap<String, OpenL>();

    public OpenL getOpenL(String name) {
        return openls.get(name);
    }

    public void registerOpenL(String name, OpenL opl) throws OpenConfigurationException {
        OpenL openl = openls.get(name);
        if (openl != null) {
            throw new OpenConfigurationException("The openl " + name + " already exists", null, null);
        }
        openls.put(name, opl);
    }

    public IOpenLConfiguration getOpenLConfiguration(String name) throws OpenConfigurationException {
        return configurations.get(name);
    }

    public void registerOpenLConfiguration(String name, IOpenLConfiguration oplc) throws OpenConfigurationException {
        IOpenLConfiguration configuration = configurations.get(name);
        if (configuration != null) {
            throw new OpenConfigurationException("The configuration " + name + " already exists", null, null);
        }
        configurations.put(name, oplc);
    }

}
