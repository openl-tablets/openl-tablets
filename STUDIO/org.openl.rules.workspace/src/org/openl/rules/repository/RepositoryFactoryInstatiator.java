package org.openl.rules.repository;

import java.util.HashMap;
import java.util.Map;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class to instantiate repository factories by class name
 * 
 * @author Yury Molchan
 */
public class RepositoryFactoryInstatiator {
    public static final String DESIGN_REPOSITORY = "design-repository.";
    public static final String PRODUCTION_REPOSITORY = "production-repository.";
    private static HashMap<String, String> oldClass;

    private static final String OLD_LOCAL_PROD = "org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory";
    private static final String OLD_LOCAL_DES = "org.openl.rules.repository.factories.LocalJackrabbitDesignRepositoryFactory";
    private static final String OLD_RMI_PROD = "org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory";
    private static final String OLD_RMI_DES = "org.openl.rules.repository.factories.RmiJackrabbitDesignRepositoryFactory";
    private static final String OLD_WEBDAV_PROD = "org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory";
    private static final String OLD_WEBDAV_DES = "org.openl.rules.repository.factories.WebDavJackrabbitDesignRepositoryFactory";
    private static final String OLD_DB = "org.openl.rules.repository.factories.DBProductionRepositoryFactory";
    private static final String NEW_LOCAL = "org.openl.rules.repository.factories.LocalJackrabbitRepositoryFactory";
    private static final String NEW_RMI = "org.openl.rules.repository.factories.RmiJackrabbitRepositoryFactory";
    private static final String NEW_WEBDAV = "org.openl.rules.repository.factories.WebDavRepositoryFactory";
    private static final String NEW_DB = "org.openl.rules.repository.factories.JdbcDBRepositoryFactory";

    static {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(OLD_LOCAL_PROD, NEW_LOCAL);
        map.put(OLD_LOCAL_DES, NEW_LOCAL);
        map.put(OLD_RMI_PROD, NEW_RMI);
        map.put(OLD_RMI_DES, NEW_RMI);
        map.put(OLD_WEBDAV_PROD, NEW_WEBDAV);
        map.put(OLD_WEBDAV_DES, NEW_WEBDAV);
        map.put(OLD_DB, NEW_DB);
        oldClass = map;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(RepositoryFactoryInstatiator.class);
    }

    /**
     * Create new instance of 'className' repository with defined configuration.
     */
    public static Repository newFactory(Map<String, Object> params, boolean designMode) throws RRepositoryException {
        ConfigSet config = new ConfigSet();
        config.addProperties(params);
        String type = designMode ? "design" : "production";
        ConfigPropertyString loginProp = new ConfigPropertyString(type + "-repository.login", null);
        ConfigPropertyString passwordProp = new ConfigPropertyString(type + "-repository.password", null);
        ConfigPropertyString uriProp = new ConfigPropertyString(type + "-repository.uri", null);
        ConfigPropertyString factoryProp = new ConfigPropertyString(type + "-repository.factory", null);

        config.updateProperty(loginProp);
        config.updatePasswordProperty(passwordProp);
        config.updateProperty(uriProp);
        config.updateProperty(factoryProp);

        String login = loginProp.getValue();
        String password = passwordProp.getValue();
        String uri = uriProp.getValue();
        String className = factoryProp.getValue();

        String clazz = checkConfig(className, config, designMode);
        RRepositoryFactory repFactory;
        try {
            // initialize
            Class<?> c = Class.forName(clazz);
            Object obj = c.getConstructor(String.class, String.class, String.class, boolean.class).newInstance(uri, login, password, designMode);
            repFactory = (RRepositoryFactory) obj;
            repFactory.initialize();
        } catch (Exception e) {
            String message = "Failed to initialize repository: " + className + " , like: " + clazz;
            log().error(message, e);
            throw new RRepositoryException(message, e);
        } catch (UnsupportedClassVersionError e) {
            String message = "Library was compiled using newer version of JDK";
            log().error(message, e);
            throw new RRepositoryException(message, e);
        }
        return repFactory;
    }

    // TODO: Remove it in 2017
    private static String checkConfig(String className, ConfigSet config, boolean designMode) {
        String type =  designMode ? DESIGN_REPOSITORY : PRODUCTION_REPOSITORY;
        checkUri(className, config, type);
        if (!oldClass.containsKey(className)) {
            // All OK!
            return className;
        }

        String clazz = oldClass.get(className);
        log().warn(
            "### Detected deprecated '{}' repository factory!\n### Use '{}' instead of.\n### To define the location of the repository use '{}uri'",
            className,
            clazz,
            type);
        return clazz;
    }

    private static void checkUri(String clazz, ConfigSet config, String type) {
        String oldUriProp = getOldUriProperty(clazz, type);
        if (oldUriProp == null) {
            // a unknown factory
            return;
        }
        ConfigPropertyString oldUri = new ConfigPropertyString(oldUriProp, null);
        config.updateProperty(oldUri);
        String oldUriValue = oldUri.getValue();
        if (oldUriValue == null) {
            // No old configuration
            return;
        }
        log().warn(
            "### Please check your configuration!\n### Deprecated '{} = {}' is being used instead of\n### '{}uri = {}'",
            oldUri.getName(),
            oldUriValue,
            type,
            oldUriValue);
        config.addProperty(type + "uri", oldUriValue);
    }

    // To support old factory names
    // TODO: Remove it in 2017
    public static String getOldUriProperty(String clazz, String type) {
        if (OLD_LOCAL_DES.equals(clazz) || OLD_LOCAL_PROD.equals(clazz) || NEW_LOCAL.equals(clazz)) {
            return type + "local.home";
        } else if (OLD_RMI_DES.equals(clazz) || OLD_RMI_PROD.equals(clazz) || NEW_RMI.equals(clazz)) {
            return type + "remote.rmi.url";
        } else if (OLD_WEBDAV_DES.equals(clazz) || OLD_WEBDAV_PROD.equals(clazz) || NEW_WEBDAV.equals(clazz)) {
            return type + "remote.webdav.url";
        } else if (OLD_DB.equals(clazz) || NEW_DB.equals(clazz)) {
            return type + "db.url";
        }
        return null;
    }

    // To support old factory names
    // TODO: Remove it in 2017
    public static String changeClassName(String className) {
        return oldClass.containsKey(className) ? oldClass.get(className) : className;
    }
}
