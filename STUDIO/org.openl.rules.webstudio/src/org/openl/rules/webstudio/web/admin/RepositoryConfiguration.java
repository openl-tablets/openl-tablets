package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.config.ReadOnlyPropertiesHolder;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private String name;
    private RepositoryType repositoryType;

    private String oldName = null;

    private String configName;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_NAME;

    private RepositorySettings settings;

    private String errorMessage;
    private final PropertiesHolder properties;
    private final String nameWithPrefix;

    public RepositoryConfiguration(String configName, PropertyResolver propertiesResolver) {
        this(configName, new ReadOnlyPropertiesHolder(propertiesResolver));
    }

    public RepositoryConfiguration(String configName, PropertiesHolder properties) {
        this.configName = configName.toLowerCase();
        this.properties = properties;
        nameWithPrefix = RepositoryInstatiator.REPOSITORY_PREFIX + configName.toLowerCase();
        REPOSITORY_FACTORY = nameWithPrefix + ".factory";
        REPOSITORY_NAME = nameWithPrefix + ".name";

        load(nameWithPrefix);
    }

    public RepositoryConfiguration(String configName,
            PropertiesHolder properties,
            RepositoryConfiguration configToClone) {
        this(configName, properties);
        // do not copy configName, only content
        setName(configToClone.getName());
        oldName = name;

        setType(configToClone.getType());
        settings.copyContent(configToClone.getSettings());
    }

    public PropertiesHolder getProperties() {
        return properties;
    }

    private void load(String configName) {
        String factoryClassName = properties.getProperty(REPOSITORY_FACTORY);
        repositoryType = RepositoryType.findByFactory(factoryClassName);
        if (repositoryType == null) {
            // Fallback to default value and save error message
            repositoryType = RepositoryType.values()[0];
            errorMessage = "Unsupported repository type. Repository factory: " + factoryClassName + ". Was replaced with " + repositoryType
                .getFactoryClassName() + ".";
        }
        name = properties.getProperty(REPOSITORY_NAME);
        oldName = name;
        settings = createSettings(repositoryType, properties, configName);
    }

    private RepositorySettings createSettings(RepositoryType repositoryType,
            PropertiesHolder properties,
            String configPrefix) {
        RepositorySettings newSettings;
        switch (repositoryType) {
            case AWS_S3:
                newSettings = new AWSS3RepositorySettings(properties, configPrefix);
                break;
            case GIT:
                newSettings = new GitRepositorySettings(properties, configPrefix);
                break;
            default:
                newSettings = new CommonRepositorySettings(properties, configPrefix, repositoryType);
                break;
        }

        return newSettings;
    }

    private void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));
        propertiesHolder.setProperty(REPOSITORY_FACTORY, repositoryType.getFactoryClassName());
        settings.store(propertiesHolder);
    }

    public void revert() {
        properties.revertProperties(REPOSITORY_NAME, REPOSITORY_FACTORY);
        load(nameWithPrefix);
        settings.revert(properties);
    }

    public void reload(){
        settings.revert(properties);
    }

    public PropertiesHolder getPropertiesToValidate() {
        InMemoryProperties tempProps = new InMemoryProperties(getProperties().getPropertyResolver());
        store(tempProps);
        return tempProps;
    }

    public void commit() {
        store(properties);
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
            RepositorySettings newSettings = createSettings(newRepositoryType, properties, nameWithPrefix);
            newSettings.copyContent(settings);
            settings = newSettings;
            settings.onTypeChanged(newRepositoryType);
        }
    }

    public String getConfigName() {
        return configName;
    }

    boolean isNameChangedIgnoreCase() {
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
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
