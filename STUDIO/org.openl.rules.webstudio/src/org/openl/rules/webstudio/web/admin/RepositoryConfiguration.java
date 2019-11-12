package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.config.ConfigurationManager;
import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.util.StringUtils;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private String name;
    private RepositoryType repositoryType;

    private String oldName = null;

    private String configName;
    private final ConfigurationManager configManager;
    private final RepositoryMode repositoryMode;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_NAME;

    private final String CONFIG_PREFIX;

    private RepositorySettings settings;

    private String errorMessage;

    public RepositoryConfiguration(String configName,
            ConfigurationManager configManager,
            RepositoryMode repositoryMode) {
        this(configName, configManager, repositoryMode, false);
    }

    public RepositoryConfiguration(String configName,
            ConfigurationManager configManager,
            RepositoryMode repositoryMode,
            boolean fallbackToDefault) {
        this.configName = configName.toLowerCase();
        this.configManager = configManager;
        this.repositoryMode = repositoryMode;

        switch (repositoryMode) {
            case DESIGN:
                CONFIG_PREFIX = RepositoryFactoryInstatiator.DESIGN_REPOSITORY;
                break;
            case DEPLOY_CONFIG:
                CONFIG_PREFIX = RepositoryFactoryInstatiator.DEPLOY_CONFIG_REPOSITORY;
                break;
            case PRODUCTION:
                CONFIG_PREFIX = RepositoryFactoryInstatiator.PRODUCTION_REPOSITORY;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        REPOSITORY_FACTORY = CONFIG_PREFIX + "factory";
        REPOSITORY_NAME = CONFIG_PREFIX + "name";

        load(fallbackToDefault);
    }

    private void load(boolean fallbackToDefault) {
        String factoryClassName = configManager.getStringProperty(REPOSITORY_FACTORY);
        repositoryType = RepositoryType.findByFactory(factoryClassName);
        if (repositoryType == null) {
            // Fallback to default value and save error message
            errorMessage = "Unsupported repository type. Repository factory: " + factoryClassName + ".";
            if (fallbackToDefault) {
                repositoryType = RepositoryType.values()[0];
                errorMessage += " Was replaced with " + repositoryType.getFactoryClassName() + ".";
            } else {
                throw new IllegalArgumentException(errorMessage);
            }
        }
        name = configManager.getStringProperty(REPOSITORY_NAME);

        settings = createSettings(repositoryType);

        fixState();
    }

    private RepositorySettings createSettings(RepositoryType repositoryType) {
        RepositorySettings newSettings;
        switch (repositoryType) {
            case AWS_S3:
                newSettings = new AWSS3RepositorySettings(configManager, CONFIG_PREFIX);
                break;
            case GIT:
                newSettings = new GitRepositorySettings(configManager, CONFIG_PREFIX, repositoryMode);
                break;
            default:
                newSettings = new CommonRepositorySettings(configManager,
                    CONFIG_PREFIX,
                    repositoryMode,
                    repositoryType);
                break;
        }

        return newSettings;
    }

    private void fixState() {
        oldName = name;
        settings.fixState();
    }

    private void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));
        propertiesHolder.setProperty(REPOSITORY_FACTORY, repositoryType.getFactoryClassName());
        settings.store(propertiesHolder);
    }

    void revert() {
        configManager.removeProperty(REPOSITORY_NAME);
        configManager.removeProperty(REPOSITORY_FACTORY);
        load(false);

        settings.revert(configManager);
    }

    void commit() {
        fixState();
        store(configManager);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormType() {
        switch (repositoryType) {
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

    public boolean isFolderRepository() {
        return repositoryType == RepositoryType.GIT;
    }

    public String getType() {
        return repositoryType.name().toLowerCase();
    }

    public void setType(String accessType) {
        RepositoryType newRepositoryType = RepositoryType.findByAccessType(accessType);
        if (repositoryType != newRepositoryType) {
            if (newRepositoryType == null) {
                throw new IllegalArgumentException(String.format("Access type %s is not supported", accessType));
            }
            repositoryType = newRepositoryType;
            errorMessage = null;
            RepositorySettings newSettings = createSettings(newRepositoryType);
            newSettings.copyContent(settings);
            settings = newSettings;
            settings.onTypeChanged(newRepositoryType);
        }
    }

    public String getConfigName() {
        return configName;
    }

    public boolean save() {
        store(configManager);
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
        InMemoryProperties propertiesHolder = new InMemoryProperties(configManager.getProperties());
        store(propertiesHolder);
        return propertiesHolder.getProperties();
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
