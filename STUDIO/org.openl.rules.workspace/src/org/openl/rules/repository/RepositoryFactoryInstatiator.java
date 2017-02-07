package org.openl.rules.repository;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.openl.config.PassCoder;
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
    public static Repository newFactory(Map<String, Object> cfg, boolean designMode) throws RRepositoryException {
        String type = designMode ? DESIGN_REPOSITORY : PRODUCTION_REPOSITORY;

        String className = get(cfg, type + "factory");
        String login = get(cfg, type + "login");
        String password = get(cfg, type + "password");
        String uri = get(cfg, type + "uri");

        String privateKay = get(cfg, "repository.encode.decode.key");
        try {
            password = PassCoder.decode(password, privateKay);
        } catch (GeneralSecurityException e) {
            throw new RRepositoryException("Can't decode the password", e);
        } catch (UnsupportedEncodingException e) {
            throw new RRepositoryException(e.getMessage(), e);
        }

        // TODO: Remove it in 2017
        String oldUriProp = getOldUriProperty(className);
        if (oldUriProp != null) {
            String oldUriValue = get(cfg, type + oldUriProp);
            if (oldUriValue != null) {
                log().warn(
                        "### Please check your configuration!\n### Deprecated '{}{} = {}' is being used instead of\n### '{}uri = {}'",
                        type,
                        oldUriProp,
                        oldUriValue,
                        type,
                        oldUriValue);
                uri = oldUriValue;
            }
        }

        // TODO: Remove it in 2017
        String clazz;
        if (!oldClass.containsKey(className)) {
            // All OK!
            clazz = className;
        } else {
            clazz = oldClass.get(className);
            log().warn(
                    "### Detected deprecated '{}' repository factory!\n### Use '{}' instead of.\n### To define the location of the repository use '{}uri'",
                    className,
                    clazz,
                    type);
        }

        try {
            Map<String, String> params = new HashMap<String, String>();
            for (Map.Entry<String, Object> entry : cfg.entrySet()) {
                if (entry.getKey().startsWith(type)) {
                    String key = entry.getKey().substring(type.length());
                    params.put(toCamelCase(key), entry.getValue().toString());
                }
            }
            params.put("uri", uri);
            params.put("login", login);
            params.put("password", password);

            return RepositoryInstatiator.newRepository(clazz, params);
        } catch (Exception e) {
            String message = "Failed to initialize repository: " + className + " , like: " + clazz;
            log().error(message, e);
            throw new RRepositoryException(message, e);
        }
    }

    /**
     * Convert parameters from "param-name" style to "paramName" style
     *
     * @param key hyphen-cased parameter name
     * @return camelCased parameter name
     */
    private static String toCamelCase(String key) {
        StringBuilder sb = new StringBuilder();
        char[] chars = key.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '-' && chars.length > i + 1) {
                i++;
                c = Character.toUpperCase(chars[i]);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String get(Map<String, Object> cfg, String key) {
        Object value = cfg.get(key);
        return value == null ? null : value.toString();
    }

    // To support old factory names
    // TODO: Remove it in 2017
    public static String getOldUriProperty(String clazz) {
        if (OLD_LOCAL_DES.equals(clazz) || OLD_LOCAL_PROD.equals(clazz) || NEW_LOCAL.equals(clazz)) {
            return "local.home";
        } else if (OLD_RMI_DES.equals(clazz) || OLD_RMI_PROD.equals(clazz) || NEW_RMI.equals(clazz)) {
            return "remote.rmi.url";
        } else if (OLD_WEBDAV_DES.equals(clazz) || OLD_WEBDAV_PROD.equals(clazz) || NEW_WEBDAV.equals(clazz)) {
            return "remote.webdav.url";
        } else if (OLD_DB.equals(clazz) || NEW_DB.equals(clazz)) {
            return "db.url";
        }
        return null;
    }

    // To support old factory names
    // TODO: Remove it in 2017
    public static String changeClassName(String className) {
        return oldClass.containsKey(className) ? oldClass.get(className) : className;
    }
}
