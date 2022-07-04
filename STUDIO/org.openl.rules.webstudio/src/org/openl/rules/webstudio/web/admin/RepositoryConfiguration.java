package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.config.ReadOnlyPropertiesHolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.util.StringUtils;
import org.springframework.core.env.PropertyResolver;

public class RepositoryConfiguration {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private String name;
    private String repoType;

    private String oldName = null;

    private final String configName;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_REF;
    private final String REPOSITORY_NAME;

    private RepositorySettings settings;

    private String errorMessage;
    private final PropertiesHolder properties;
    private final String nameWithPrefix;

    private FreeValueFinder valueFinder;
    private RepositoryMode repoMode;

    public RepositoryConfiguration(String configName, PropertyResolver propertiesResolver) {
        this(configName, new ReadOnlyPropertiesHolder(propertiesResolver));
    }

    public RepositoryConfiguration(String configName, PropertiesHolder properties) {
        this.configName = configName.toLowerCase();
        this.properties = properties;
        nameWithPrefix = Comments.REPOSITORY_PREFIX + configName.toLowerCase();
        REPOSITORY_FACTORY = nameWithPrefix + ".factory";
        REPOSITORY_REF = nameWithPrefix + ".$ref";
        REPOSITORY_NAME = nameWithPrefix + ".name";

        load();
    }

    /**
     * This constructor should be called for creating a new configuration.
     */
    public RepositoryConfiguration(String configName,
                                   PropertiesHolder properties,
                                   String type,
                                   FreeValueFinder valueFinder,
                                   RepositoryMode repoMode) {
        this(configName, properties);
        this.valueFinder = valueFinder;
        this.repoMode = repoMode;

        // Define default settings for the new repository
        String defaultSettingsPrefix = "repo-default." + repoMode.getId();
        properties.setProperty(nameWithPrefix + ".comment-template.$ref", defaultSettingsPrefix + ".comment-template");
        properties.setProperty(nameWithPrefix + ".base.path.$ref", defaultSettingsPrefix + ".base.path");
        if (repoMode.equals(RepositoryMode.DESIGN)) {
            properties.setProperty(nameWithPrefix + ".new-branch.$ref", defaultSettingsPrefix + ".new-branch");
        }

        String defValue = properties.getProperty(defaultSettingsPrefix + ".name");
        setName(valueFinder.find("name", defValue));
        oldName = name;

        repoType = ""; // To force "type is changed" event in the next step
        setType(type);
    }

    public PropertiesHolder getProperties() {
        return properties;
    }

    private void load() {
        String factoryClassName = properties.getProperty(REPOSITORY_FACTORY);
        repoType = RepositoryInstatiator.getRefID(factoryClassName);
        RepositoryType repositoryType = RepositoryType.findByFactory(repoType);
        if (repositoryType == null) {
            // Fallback to default value
            repositoryType = RepositoryType.GIT;
            repoType = repositoryType.factoryId;
            if (factoryClassName != null) {
                //add error message
                errorMessage = "Unsupported repository type. Repository factory: " + factoryClassName + ". Was replaced with " + repoType + ".";
            }
        }
        name = properties.getProperty(REPOSITORY_NAME);
        oldName = name;
        settings = createSettings(repositoryType, properties, nameWithPrefix);
    }

    private RepositorySettings createSettings(RepositoryType repositoryType,
            PropertiesHolder properties,
            String configPrefix) {
        RepositorySettings newSettings;
        switch (repositoryType) {
            case AWS_S3:
                newSettings = new AWSS3RepositorySettings(properties, configPrefix);
                break;
            case AZURE:
                newSettings = new AzureBlobRepositorySettings(properties, configPrefix);
                break;
            case GIT:
                if (repoMode != null) {
                    // Generate a unique path for a just created GIT configuration
                    String defValue = properties.getProperty("repo-default." + repoMode.getId() + ".local-repository-path");
                    properties.setProperty(nameWithPrefix + ".local-repository-path", valueFinder.find("local-repository-path", defValue));
                }
                newSettings = new GitRepositorySettings(properties, configPrefix);
                break;
            case LOCAL:
                newSettings = new LocalRepositorySettings(properties, configPrefix);
                break;
            default:
                newSettings = new CommonRepositorySettings(properties, configPrefix);
                break;
        }

        return newSettings;
    }

    private void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));

        String factoryId = Objects.requireNonNull(RepositoryType.findByFactory(repoType)).factoryId;
        propertiesHolder.setProperty(REPOSITORY_REF, factoryId);

        settings.store(propertiesHolder);
    }

    public void revert() {
        properties.revertProperties(REPOSITORY_NAME, REPOSITORY_REF);
        load();
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

    public boolean isFolderRepository() {
        return RepositoryType.GIT.factoryId.equals(repoType);
    }

    public String getType() {
        return repoType;
    }

    public RepositoryType getRepositoryType() {
        return RepositoryType.findByFactory(repoType);
    }

    public void setType(String newRepoType) {
        if (StringUtils.isEmpty(newRepoType)) {
            return;
        }
        if (!repoType.equals(newRepoType)) {
            RepositoryType newRepositoryType = RepositoryType.findByFactory(newRepoType);

            if (newRepositoryType == null) {
                throw new IllegalArgumentException(String.format("Access type '%s' is not supported", newRepoType));
            }
            repoType = newRepoType;
            errorMessage = null;

            properties.setProperty(REPOSITORY_REF, newRepositoryType.factoryId);
            settings = createSettings(newRepositoryType, properties, nameWithPrefix);
        }
    }

    public String getId() {
        return getConfigName();
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
