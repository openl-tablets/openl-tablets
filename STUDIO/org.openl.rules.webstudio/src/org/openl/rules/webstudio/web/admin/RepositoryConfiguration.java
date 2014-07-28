package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.openl.config.ConfigurationManager;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();
    public static final String SECURE_CONFIG_FILE = "/secure-jackrabbit-repository.xml";

    private ConfigurationManager configManager;

    private static final String PRODUCTION_REPOSITORY_FACTORY = "production-repository.factory";
    private static final String PRODUCTION_REPOSITORY_NAME = "production-repository.name";

    private static final String PRODUCTION_REPOSITORY_LOGIN = "production-repository.login";
    private static final String PRODUCTION_REPOSITORY_PASS = "production-repository.password";
    
    private static final String PRODUCTION_REPOSITORY_CONFIG_FILE = "production-repository.config";

    private static final String PRODUCTION_REPOSITORY_CONNECTION_TYPE = "production-repository.connection.type";

    private boolean secure = false;
    /** @deprecated */
    private static final BidiMap PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP = new DualHashBidiMap();
    static {
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("local",
                "org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("rmi",
                "org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory");
        PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.put("webdav",
                "org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory");
    }
    /** @deprecated */
    private static final Map<String, String> PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP = new HashMap<String, String>();
    static {
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("local", "production-repository.local.home");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("rmi", "production-repository.remote.rmi.url");
        PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.put("webdav", "production-repository.remote.webdav.url");
    }

    private String configName;

    private String oldName = null;

    public RepositoryConfiguration(String configName, ConfigurationManager configManager) {
        this.configName = configName.toLowerCase();
        this.configManager = configManager;
    }

    public String getName() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_NAME);
    }

    public void setName(String name) {
        oldName = getName();
        configManager.setProperty(PRODUCTION_REPOSITORY_NAME, StringUtils.trimToEmpty(name));
    }

    public String getType() {
        String factory = configManager.getStringProperty(PRODUCTION_REPOSITORY_FACTORY);
        return (String) PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.getKey(factory);
    }

    public void setType(String type) {
        configManager.setProperty(PRODUCTION_REPOSITORY_FACTORY, PRODUCTION_REPOSITORY_TYPE_FACTORY_MAP.get(type));
    }

    public String getPath() {
        String type = getType();
        String propName = PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type);

        return "local".equals(type) ?
                configManager.getPath(propName) : configManager.getStringProperty(propName);
    }

    public void setPath(String path) {
        String type = getType();
        String propName = PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type);
        String normalizedPath = StringUtils.trimToEmpty(path);

        if ("local".equals(type)) {
            configManager.setPath(propName, normalizedPath);
        } else {
            configManager.setProperty(propName, normalizedPath);
        }
    }

    public String getConfigName() {
        return configName;
    }

    public boolean save() {
        return configManager.save();
    }

    public boolean restoreDefaults() {
        return configManager.restoreDefaults();
    }

    public boolean delete() {
        return configManager.delete();
    }

    public void copyContent(RepositoryConfiguration other) {
        // do not copy configName, only content
        setName(other.getName());
        setType(other.getType());
        setPath(other.getPath());
    }

    public boolean isNameChanged() {
        String name = getName();
        return name != null && !name.equals(oldName) || name == null && oldName != null;
    }

    public boolean isNameChangedIgnoreCase() {
        String name = getName();
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
    }

    public boolean isProductionRepositoryPathSystem() {
        String type = getType();
        return configManager.isSystemProperty(PRODUCTION_REPOSITORY_TYPE_PATH_PROPERTY_MAP.get(type));
    }

    public String getLogin() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_LOGIN);
    }

    public void setLogin(String login) {
        configManager.setProperty(PRODUCTION_REPOSITORY_LOGIN, login);
    }

    public String getPassword() {
        return "";
        //return configManager.getPassword(PRODUCTION_REPOSITORY_PASS);
    }

    public void setPassword(String pass) {
        if (!StringUtils.isEmpty(pass)) {
            configManager.setPassword(PRODUCTION_REPOSITORY_PASS, pass);
        }
    }

    public String getConnectionType() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_CONNECTION_TYPE);
    }

    public void setConnectionType(String connectionType) {
        configManager.setProperty(PRODUCTION_REPOSITORY_CONNECTION_TYPE, connectionType);
    }

    public Map<String, Object> getProperties() {
        return configManager.getProperties();
    }

    public String getConfigFile() {
        return configManager.getStringProperty(PRODUCTION_REPOSITORY_CONFIG_FILE);
    }

    public void setConfigFile(String configFile) {
        configManager.setProperty(PRODUCTION_REPOSITORY_CONFIG_FILE, configFile);
    }

    public boolean isSecure() {
        return secure || !StringUtils.isEmpty(this.getLogin());
    }

    public void setSecure(boolean secure) {
        if (!secure) {
            configManager.removeProperty(PRODUCTION_REPOSITORY_LOGIN);
            configManager.removeProperty(PRODUCTION_REPOSITORY_PASS);
            configManager.removeProperty(PRODUCTION_REPOSITORY_CONFIG_FILE);
        } else {
            configManager.setProperty(PRODUCTION_REPOSITORY_CONFIG_FILE, SECURE_CONFIG_FILE);
        }
        this.secure = secure;
    }

    protected static class NameWithNumbersComparator implements Comparator<RepositoryConfiguration> {
        private static final Pattern pattern = Pattern.compile("([^\\d]*+)(\\d*+)");

        @Override
        public int compare(RepositoryConfiguration o1, RepositoryConfiguration o2) {
            Matcher m1 = pattern.matcher(o1.getName());
            Matcher m2 = pattern.matcher(o2.getName());
            while (true) {
                boolean f1 = m1.find();
                boolean f2 = m2.find();
                if (!f1 && !f2)
                    return 0;
                if (f1 != f2) {
                    return f1 ? 1 : -1;
                }
                
                String s1 = m1.group(1);
                String s2 = m2.group(1);
                int compare = s1.compareToIgnoreCase(s2);
                if (compare != 0) {
                    return compare;
                }
                
                String n1 = m1.group(2);
                String n2 = m2.group(2);
                if (!n1.equals(n2)) {
                    if (n1.isEmpty())
                        return -1;
                    if (n2.isEmpty())
                        return 1;
                    return new BigInteger(n1).compareTo(new BigInteger(n2));
                }
            }
        }
    }
}
