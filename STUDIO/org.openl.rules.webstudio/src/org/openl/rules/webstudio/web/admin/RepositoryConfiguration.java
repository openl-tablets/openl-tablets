package org.openl.rules.webstudio.web.admin;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.PropertyResolver;

import org.openl.config.InMemoryProperties;
import org.openl.config.PropertiesHolder;
import org.openl.config.ReadOnlyPropertiesHolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.Props;
import org.openl.studio.settings.converter.SettingPropertyName;
import org.openl.util.StringUtils;

@Slf4j
public class RepositoryConfiguration implements ConfigPrefixSettingsHolder {
    public static final Comparator<RepositoryConfiguration> COMPARATOR = new NameWithNumbersComparator();

    private static final String REPOSITORY_NAME_SUFFIX = ".name";
    private static final String REPOSITORY_FACTORY_SUFFIX = ".factory";
    public static final String REPOSITORY_DEFAULT_PREFIX = "repo-default.";

    @Getter
    @Setter
    @SettingPropertyName(suffix = REPOSITORY_NAME_SUFFIX)
    private String name;
    private String repoType;

    @JsonIgnore
    private String oldName = null;

    @Getter
    @JsonIgnore
    private final String configName;

    private final String REPOSITORY_FACTORY;
    private final String REPOSITORY_REF;
    private final String REPOSITORY_NAME;

    @Getter
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
    @Valid
    private RepositorySettings settings;

    @Getter
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String errorMessage;

    @Getter
    @JsonIgnore
    private final PropertiesHolder properties;

    @JsonIgnore
    private final String nameWithPrefix;

    private BiFunction<String, String, String> valueFinder;

    @JsonIgnore
    private RepositoryMode repoMode;

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
        var defaultSettingsPrefix = REPOSITORY_DEFAULT_PREFIX + repoMode.getId();
        properties.setProperty(nameWithPrefix + ".comment-template.$ref", defaultSettingsPrefix + ".comment-template");
        properties.setProperty(nameWithPrefix + ".base.path.$ref", defaultSettingsPrefix + ".base.path");
        if (repoMode.equals(RepositoryMode.DESIGN)) {
            properties.setProperty(nameWithPrefix + ".new-branch.$ref", defaultSettingsPrefix + ".new-branch");
            properties.setProperty(nameWithPrefix + RepositorySettings.PROJECT_DISCOVERY_SUFFIX + ".$ref",
                    defaultSettingsPrefix + RepositorySettings.PROJECT_DISCOVERY_SUFFIX);
        }

        var defValue = properties.getProperty(defaultSettingsPrefix + ".name");
        setName(valueFinder.apply("name", defValue));
        oldName = name;

        repoType = ""; // To force "type is changed" event in the next step
        setType(type);
    }

    private void load() {
        var factoryClassName = properties.getProperty(REPOSITORY_FACTORY);
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
        settings = createSettings(repositoryType, properties, nameWithPrefix, false);
    }

    private RepositoryMode getRepoMode() {
        if (repoMode != null) {
            return repoMode;
        }
        Predicate<String> containsConfiguration = reposConfig -> {
            var reposConfigs = StringUtils.split(properties.getProperty(reposConfig), ',');
            if (reposConfigs != null) {
                for (String designRepoConfig : reposConfigs) {
                    if (configName.equalsIgnoreCase(designRepoConfig)) {
                        return true;
                    }
                }
            }
            return false;
        };

        if (containsConfiguration.test(AdministrationSettings.DESIGN_REPOSITORY_CONFIGS)) {
            return RepositoryMode.DESIGN;
        } else if (containsConfiguration.test(AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS)) {
            return RepositoryMode.PRODUCTION;
        }
        return null;
    }

    private RepositorySettings createSettings(RepositoryType repositoryType,
                                              PropertiesHolder properties,
                                              String configPrefix,
                                              boolean createUniquePath) {
        // Generate a unique path for a just created GIT configuration

        var repositoryMode = getRepoMode();
        return switch (repositoryType) {
            case AWS_S3 -> new AWSS3RepositorySettings(properties, configPrefix, repositoryMode);
            case AZURE -> new AzureBlobRepositorySettings(properties, configPrefix, repositoryMode);
            case GIT -> {
                if (createUniquePath) {
                    // Generate a unique path for a just created GIT configuration
                    var defValue = properties.getProperty(REPOSITORY_DEFAULT_PREFIX + repositoryMode.getId() + GitRepositorySettings.URI_SUFFIX);
                    properties.setProperty(nameWithPrefix + GitRepositorySettings.URI_SUFFIX, valueFinder.apply("uri", defValue));
                }
                yield new GitRepositorySettings(properties, configPrefix, repositoryMode);
            }
            case LOCAL -> new LocalRepositorySettings(properties, configPrefix, repositoryMode);
            default -> new CommonRepositorySettings(properties, configPrefix, repositoryMode);
        };
    }

    private void store(PropertiesHolder propertiesHolder) {
        propertiesHolder.setProperty(REPOSITORY_NAME, StringUtils.trimToEmpty(name));

        var factoryId = Objects.requireNonNull(RepositoryType.findByFactory(repoType)).factoryId;
        propertiesHolder.setProperty(REPOSITORY_REF, factoryId);

        settings.store(propertiesHolder);
    }

    public void revert() {
        properties.revertProperties(REPOSITORY_NAME,
                REPOSITORY_REF);
        load();
        settings.revert(properties);
    }

    @JsonIgnore
    public PropertiesHolder getPropertiesToValidate() {
        var tempProps = new InMemoryProperties(getProperties().getPropertyResolver());
        store(tempProps);
        return tempProps;
    }

    public void commit() {
        store(properties);
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
                throw new IllegalArgumentException("Access type '%s' is not supported".formatted(newRepoType));
            }
            repoType = newRepoType;
            errorMessage = null;

            properties.setProperty(REPOSITORY_REF, newRepositoryType.factoryId);
            settings = createSettings(newRepositoryType, properties, nameWithPrefix, true);
        }
    }

    @JsonView({RepositorySettings.Views.Design.class, RepositorySettings.Views.Production.class})
    public String getId() {
        return getConfigName();
    }

    boolean isNameChangedIgnoreCase() {
        return name != null && !name.equalsIgnoreCase(oldName) || name == null && oldName != null;
    }

    protected static class NameWithNumbersComparator implements Comparator<RepositoryConfiguration> {
        private static final Pattern pattern = Pattern.compile("([^\\d]*+)(\\d*+)");

        @Override
        public int compare(RepositoryConfiguration o1, RepositoryConfiguration o2) {
            var m1 = pattern.matcher(o1.getName());
            var m2 = pattern.matcher(o2.getName());
            while (true) {
                var f1 = m1.find();
                var f2 = m2.find();
                if (!f1 && !f2) {
                    return 0;
                }
                if (f1 != f2) {
                    return f1 ? 1 : -1;
                }

                var s1 = m1.group(1);
                var s2 = m2.group(1);
                var compare = s1.compareToIgnoreCase(s2);
                if (compare != 0) {
                    return compare;
                }

                var n1 = m1.group(2);
                var n2 = m2.group(2);
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
            var max = new AtomicInteger(-1);
            var configName = repoMode.getId();
            var configNames = configurations.stream().map(RepositoryConfiguration::getConfigName).collect(Collectors
                    .toSet());

            //existingConfigNames can contain ids that were deleted but were not saved, such ids should not be assigned to a new repository
            var existingConfigNames = Props.getEnvironment().getProperty(configName + "-repository-configs");
            if (StringUtils.isNotEmpty(existingConfigNames)) {
                configNames.addAll(Arrays.asList(existingConfigNames.split(",")));
            }

            configNames.forEach(rc -> configurations.forEach(configuration -> {
                var repoValue = configuration.getPropertiesToValidate()
                        .getProperty(Comments.REPOSITORY_PREFIX + rc + "." + paramNameSuffix);
                if (repoValue != null && repoValue.startsWith(defValue)) {
                    final var suffix = repoValue.substring(defValue.length());
                    if (suffix.matches("\\d*")) {
                        try {
                            int i = suffix.isEmpty() ? 0 : Integer.parseInt(suffix);
                            if (i > max.get()) {
                                max.set(i);
                            }
                        } catch (NumberFormatException e) {
                            // Perhaps the number is greater than the Integer.MAX_VALUE, ignore this value
                            log.debug("Ignored error while forming the config name: ", e);
                        }
                    }
                }
            }));
            var index = max.get();
            return index >= 0 && index < Integer.MAX_VALUE ? defValue + (max.incrementAndGet()) : defValue;
        };
    }

    @Override
    public String getConfigPropertyKey(String configSuffix) {
        return Comments.REPOSITORY_PREFIX + configName + configSuffix;
    }
}
