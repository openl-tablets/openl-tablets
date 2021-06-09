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
 * The class is designed as immutable, but not immutable because contains ClassLoader.
 *
 * @author snshor
 *
 */
public final class UserContext extends AUserContext {

    private final ClassLoader userClassLoader;

    private final String userHome;

    public UserContext(ClassLoader userClassLoader, String userHome) {
        this.userClassLoader = userClassLoader;
        this.userHome = new File(userHome).getAbsolutePath();
    }

    @Override
    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    @Override
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
        return "home=" + userHome + "cl=" + printClassloader(userClassLoader);
    }

    private final Map<String, IOpenLConfiguration> configurations = new HashMap<>();

    private final Map<String, OpenL> openls = new HashMap<>();

    @Override
    public OpenL getOpenL(String name) {
        return openls.get(name);
    }

    @Override
    public void registerOpenL(String name, OpenL opl) {
        OpenL openl = openls.get(name);
        if (openl != null) {
            throw new OpenLConfigurationException(String.format("The openl %s already exists", name), null, null);
        }
        openls.put(name, opl);
    }

    @Override
    public IOpenLConfiguration getOpenLConfiguration(String name) {
        return configurations.get(name);
    }

    @Override
    public void registerOpenLConfiguration(String name, IOpenLConfiguration oplc) {
        IOpenLConfiguration configuration = configurations.get(name);
        if (configuration != null) {
            throw new OpenLConfigurationException(String.format("The configuration %s already exists", name),
                null,
                null);
        }
        configurations.put(name, oplc);
    }

}
