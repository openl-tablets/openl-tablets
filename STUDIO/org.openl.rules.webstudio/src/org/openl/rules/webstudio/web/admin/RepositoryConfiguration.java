package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.config.ConfigurationManager;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.util.StringUtils;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private String name;
    private JcrType jcrType;

    private String oldName = null;

    private String configName;
    private final ConfigurationManager configManager;
    private final RepositoryType repositoryType;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_NAME;

    private final String CONFIG_PREFIX;

    private RepositorySettings settings;

    public RepositoryConfiguration(String configName,
            ConfigurationManager configManager,
            RepositoryType repositoryType) {
        this.configName = configName.toLowerCase();
        this.configManager = configManager;
        this.repositoryType = repositoryType;

        CONFIG_PREFIX = repositoryType == RepositoryType.DESIGN ? RepositoryFactoryInstatiator.DESIGN_REPOSITORY
                                                                : RepositoryFactoryInstatiator.PRODUCTION_REPOSITORY;
        REPOSITORY_FACTORY = CONFIG_PREFIX + "factory";
        REPOSITORY_NAME = CONFIG_PREFIX + "name";


        load();
    }

    private void load() {
        String factoryClassName = configManager.getStringProperty(REPOSITORY_FACTORY);
        jcrType = JcrType.findByFactory(factoryClassName);
        name = configManager.getStringProperty(REPOSITORY_NAME);

        settings = createSettings(jcrType);

        fixState();
    }

    private RepositorySettings createSettings(JcrType jcrType) {
        String factoryClassName = configManager.getStringProperty(REPOSITORY_FACTORY);
        RepositorySettings newSettings;
        switch (jcrType) {
            case AWS_S3:
                newSettings = new AWSS3RepositorySettings(configManager, CONFIG_PREFIX);
                break;
            default:
                newSettings = new CommonRepositorySettings(configManager, CONFIG_PREFIX, factoryClassName, repositoryType, jcrType);
                break;
        }

        return newSettings;
    }

    private void fixState() {
        oldName = name;
        settings.fixState();
    }

    private void store() {
        configManager.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));
        configManager.setProperty(REPOSITORY_FACTORY, jcrType.getFactoryClassName());
        settings.store(configManager);
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

    public String getFormType() {
        switch (jcrType) {
            case LOCAL:
            case RMI:
            case WEBDAV:
            case DB:
            case JNDI:
                return "common";
            default:
                return getType();
        }
    }

    public String getType() {
        return jcrType.name().toLowerCase();
    }

    public void setType(String accessType) {
        JcrType newJcrType = JcrType.findByAccessType(accessType);
        if (jcrType != newJcrType) {
            jcrType = newJcrType;
            RepositorySettings newSettings = createSettings(newJcrType);
            newSettings.copyContent(settings);
            settings = newSettings;
            settings.onTypeChanged(newJcrType);
        }
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
        settings.copyContent(other.getSettings());
        fixState();
    }

    public boolean isNameChangedIgnoreCase() {
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
    }

    public Map<String, Object> getProperties() {
        store();
        return configManager.getProperties();
    }

    public RepositorySettings getSettings() {
        return settings;
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
