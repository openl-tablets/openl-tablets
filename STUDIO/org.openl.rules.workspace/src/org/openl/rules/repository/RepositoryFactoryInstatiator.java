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
    public static final String DEPLOY_CONFIG_REPOSITORY = "deploy-config-repository.";
    public static final String PRODUCTION_REPOSITORY = "production-repository.";

    private static Logger log() {
        return LoggerFactory.getLogger(RepositoryFactoryInstatiator.class);
    }

    /**
     * Create new instance of 'className' repository with defined configuration.
     */
    public static Repository newFactory(Map<String, Object> cfg,
            RepositoryMode repositoryMode) throws RRepositoryException {
        String type;
        switch (repositoryMode) {
            case DESIGN:
                type = DESIGN_REPOSITORY;
                break;
            case DEPLOY_CONFIG:
                type = DEPLOY_CONFIG_REPOSITORY;
                break;
            case PRODUCTION:
                type = PRODUCTION_REPOSITORY;
                break;
            default:
                throw new UnsupportedOperationException();
        }

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

        try {
            Map<String, String> params = new HashMap<>();
            for (Map.Entry<String, Object> entry : cfg.entrySet()) {
                if (entry.getKey().startsWith(type)) {
                    String key = entry.getKey().substring(type.length());
                    params.put(toCamelCase(key), entry.getValue().toString());
                }
            }
            params.put("uri", uri);
            params.put("login", login);
            params.put("password", password);

            return RepositoryInstatiator.newRepository(className, params);
        } catch (Exception e) {
            String message = "Failed to initialize repository: " + className;
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
}
