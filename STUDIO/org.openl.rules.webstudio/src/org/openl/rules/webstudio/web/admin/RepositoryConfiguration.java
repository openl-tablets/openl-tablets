package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.config.ReadOnlyPropertiesHolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.workspace.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.util.StringUtils;

public class RepositoryConfiguration implements ConfigPrefixSettingsHolder {
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryConfiguration.class);
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private static final String REPOSITORY_NAME_SUFFIX = ".name";
    private static final String REPOSITORY_FACTORY_SUFFIX = ".factory";

    @SettingPropertyName(suffix = REPOSITORY_NAME_SUFFIX)
    private String name;
    private String repoType;

    @JsonIgnore
    private String oldName = null;

    @JsonIgnore
    private final String configName;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_REF;
    private final String REPOSITORY_NAME;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @Valid
    private RepositorySettings settings;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String errorMessage;

    @JsonIgnore
    private final PropertiesHolder properties;

    @JsonIgnore
    private final String nameWithPrefix;

    private BiFunction<String, String, String> valueFinder;

    private RepositoryMode repoMode;

    @JsonView(RepositorySettings.Views.DeployConfig.class)
    @SettingPropertyName(value = DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG)
    private String useDesignRepositoryForDeployConfig;

    public RepositoryConfiguration(String configName, PropertyResolver propertiesResolver) {
        this(configName, new ReadOnlyPropertiesHolder(propertiesResolver));
    }

    public RepositoryConfiguration(String configName, PropertiesHolder properties) {
        this.configName = configName.toLowerCase();
        this.properties = properties;
        nameWithPrefix = Comments.REPOSITORY_PREFIX + configName.toLowerCase();
        REPOSITORY_FACTORY = nameWithPrefix + REPOSITORY_FACTORY_SUFFIX;
        REPOSITORY_REF = nameWithPrefix + ".$ref";
        REPOSITORY_NAME = nameWithPrefix + REPOSITORY_NAME_SUFFIX;

        load();
    }

    /**
     * This constructor should be called for creating a new configuration.
     */
    public RepositoryConfiguration(String configName,
                                   PropertiesHolder properties,
                                   String type,
                                   List<RepositoryConfiguration> configurations,
                                   RepositoryMode repoMode) {
        this(configName, properties);
        this.valueFinder = createValueFinder(configurations, repoMode);
        this.repoMode = repoMode;

        // Define default settings for the new repository
        String defaultSettingsPrefix = "repo-default." + repoMode.getId();
        properties.setProperty(nameWithPrefix + ".comment-template.$ref", defaultSettingsPrefix + ".comment-template");
        properties.setProperty(nameWithPrefix + ".base.path.$ref", defaultSettingsPrefix + ".base.path");
        if (repoMode.equals(RepositoryMode.DESIGN)) {
            properties.setProperty(nameWithPrefix + ".new-branch.$ref", defaultSettingsPrefix + ".new-branch");
        }

        String defValue = properties.getProperty(defaultSettingsPrefix + ".name");
        setName(valueFinder.apply("name", defValue));
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

        if (isDeployConfig()) {
            useDesignRepositoryForDeployConfig = properties.getProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG);
        }
    }

    private RepositorySettings createSettings(RepositoryType repositoryType,
                                              PropertiesHolder properties,
                                              String configPrefix) {
        // Generate a unique path for a just created GIT configuration

        return switch (repositoryType) {
            case AWS_S3 -> new AWSS3RepositorySettings(properties, configPrefix);
            case AZURE -> new AzureBlobRepositorySettings(properties, configPrefix);
            case GIT -> {
                if (repoMode != null) {
                    // Generate a unique path for a just created GIT configuration
                    String defValue = properties.getProperty("repo-default." + repoMode.getId() + ".local-repository-path");
                    properties.setProperty(nameWithPrefix + ".local-repository-path", valueFinder.apply("local-repository-path", defValue));
                }
                yield new GitRepositorySettings(properties, configPrefix);
            }
            case LOCAL -> new LocalRepositorySettings(properties, configPrefix);
            default -> new CommonRepositorySettings(properties, configPrefix);
        };
    }

    private void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));

        String factoryId = Objects.requireNonNull(RepositoryType.findByFactory(repoType)).factoryId;
        propertiesHolder.setProperty(REPOSITORY_REF, factoryId);

        if (isDeployConfig()) {
            propertiesHolder.setProperty(DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG, useDesignRepositoryForDeployConfig);
        }

        settings.store(propertiesHolder);
    }

    public void revert() {
        properties.revertProperties(REPOSITORY_NAME,
                REPOSITORY_REF,
                DesignTimeRepositoryImpl.USE_REPOSITORY_FOR_DEPLOY_CONFIG);
        load();
        settings.revert(properties);
    }

    private boolean isDeployConfig() {
        return configName.equalsIgnoreCase(RepositoryMode.DEPLOY_CONFIG.getId());
    }

    @JsonIgnore
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

    @JsonIgnore
    public boolean isFolderRepository() {
        return RepositoryType.GIT.factoryId.equals(repoType);
    }

    @SettingPropertyName(suffix = REPOSITORY_FACTORY_SUFFIX)
    public String getType() {
        return repoType;
    }

    @JsonIgnore
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

    @JsonView({RepositorySettings.Views.Design.class, RepositorySettings.Views.Production.class})
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

    public String getUseDesignRepositoryForDeployConfig() {
        return useDesignRepositoryForDeployConfig;
    }

    public void setUseDesignRepositoryForDeployConfig(String useDesignRepositoryForDeployConfig) {
        this.useDesignRepositoryForDeployConfig = useDesignRepositoryForDeployConfig;
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

    private static BiFunction<String, String, String> createValueFinder(List<RepositoryConfiguration> configurations, RepositoryMode repoMode) {
        return (paramNameSuffix, defValue) -> {
            AtomicInteger max = new AtomicInteger(-1);
            String configName = repoMode.getId();
            Set<String> configNames = configurations.stream().map(RepositoryConfiguration::getConfigName).collect(Collectors
                    .toSet());

            //existingConfigNames can contain ids that were deleted but were not saved, such ids should not be assigned to a new repository
            String existingConfigNames = Props.getEnvironment().getProperty(configName + "-repository-configs");
            if (StringUtils.isNotEmpty(existingConfigNames)) {
                configNames.addAll(Arrays.asList(existingConfigNames.split(",")));
            }

            configNames.forEach(rc -> configurations.forEach(configuration -> {
                String repoValue = configuration.getPropertiesToValidate()
                        .getProperty(Comments.REPOSITORY_PREFIX + rc + "." + paramNameSuffix);
                if (repoValue != null && repoValue.startsWith(defValue)) {
                    final String suffix = repoValue.substring(defValue.length());
                    if (suffix.matches("\\d*")) {
                        try {
                            int i = suffix.isEmpty() ? 0 : Integer.parseInt(suffix);
                            if (i > max.get()) {
                                max.set(i);
                            }
                        } catch (NumberFormatException e) {
                            // Perhaps the number is greater than the Integer.MAX_VALUE, ignore this value
                            LOG.debug("Ignored error while forming the config name: ", e);
                        }
                    }
                }
            }));
            int index = max.get();
            return index >= 0 && index < Integer.MAX_VALUE ? defValue + (max.incrementAndGet()) : defValue;
        };
    }

    @Override
    public String getConfigPropertyKey(String configSuffix) {
        return Comments.REPOSITORY_PREFIX + configName + configSuffix;
    }
}
