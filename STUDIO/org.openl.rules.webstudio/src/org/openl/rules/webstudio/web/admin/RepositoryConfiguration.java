package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.openl.config.ConfigurationManager;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private String login;
    private String password;
    private String uri;
    private String name;
    private JcrType jcrType;

    private boolean secure = false;
    private String oldName = null;
    private JcrType oldJcrType = null;

    private String configName;
    private final ConfigurationManager configManager;
    private final RepositoryType repositoryType;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_NAME;

    private final String REPOSITORY_URI;
    private final String REPOSITORY_LOGIN;
    private final String REPOSITORY_PASS;



    public RepositoryConfiguration(String configName, ConfigurationManager configManager, RepositoryType repositoryType) {
        this.configName = configName.toLowerCase();
        this.configManager = configManager;
        this.repositoryType = repositoryType;

        String repoType = repositoryType.toString();
        REPOSITORY_FACTORY = repoType + "-repository.factory";
        REPOSITORY_NAME = repoType + "-repository.name";

        REPOSITORY_URI = repoType + "-repository.uri";
        REPOSITORY_LOGIN = repoType + "-repository.login";
        REPOSITORY_PASS = repoType + "-repository.password";

        load();
    }

    private void load() {
        jcrType = JcrType.findByFactory(configManager.getStringProperty(REPOSITORY_FACTORY));
        name = configManager.getStringProperty(REPOSITORY_NAME);
        uri = jcrType == JcrType.LOCAL ? configManager.getPath(REPOSITORY_URI) : configManager.getStringProperty(REPOSITORY_URI);
        login = configManager.getStringProperty(REPOSITORY_LOGIN);

        fixState();
    }

    private void fixState() {
        secure = StringUtils.isNotEmpty(login);
        oldName = name;
        oldJcrType = jcrType;
    }

    private void store() {
        configManager.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));
        configManager.setProperty(REPOSITORY_FACTORY, jcrType.getFactoryClassName());
        if (jcrType == JcrType.LOCAL) {
            configManager.setPath(REPOSITORY_URI, uri);
        } else {
            configManager.setProperty(REPOSITORY_URI, uri);
        }

        if (!secure) {
            configManager.removeProperty(REPOSITORY_LOGIN);
            configManager.removeProperty(REPOSITORY_PASS);
        } else {
            if (!StringUtils.isEmpty(password)) {
                configManager.setProperty(REPOSITORY_LOGIN, login);
                configManager.setPassword(REPOSITORY_PASS, password);
            }
        }
    }

    void commit() {
        fixState();
        store();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return jcrType.name().toLowerCase();
    }

    public void setType(String accessType) {
        this.jcrType = JcrType.findByAccessType(accessType);
    }

    public String getPath() {
        // Default values
        if (StringUtils.isEmpty(uri) || oldJcrType != jcrType) {
            String type = repositoryType == RepositoryType.DESIGN ? "design" : "deployment";
            switch (jcrType) {
                case LOCAL:
                    return "../" + type + "-repository";
                case RMI:
                    return "//localhost:1099/" + type + "-repository";
                case WEBDAV:
                    return "http://localhost:8080/" + type + "-repository";
                case DB:
                    return "jdbc:mysql://localhost:3306/" + type + "-repository";
                case JNDI:
                    return "java:comp/env/jdbc/" + type + "DB";
            }
        }
        return uri;
    }

    public void setPath(String path) {
        this.uri = StringUtils.trimToEmpty(path);
    }

    public String getConfigName() {
        return configName;
    }

    public boolean save() {
        store();
        return configManager.save();
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

    public boolean isNameChangedIgnoreCase() {
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
    }

    public boolean isRepositoryPathSystem() {
        return configManager.isSystemProperty(REPOSITORY_URI);
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return "";
    }

    public void setPassword(String pass) {
        this.password = pass;
    }

    public Map<String, Object> getProperties() {
        store();
        return configManager.getProperties();
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
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
                if (!f1 && !f2) {
                    return 0;
                }
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
                    if (n1.isEmpty()) {
                        return -1;
                    }
                    if (n2.isEmpty()) {
                        return 1;
                    }
                    return new BigInteger(n1).compareTo(new BigInteger(n2));
                }
            }
        }
    }
}
