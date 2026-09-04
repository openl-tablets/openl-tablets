package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.Getter;

import org.openl.config.PropertiesHolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.util.StringUtils;

public class RepositoryEditor {

    private final RepositoryFactoryProxy repositoryFactoryProxy;
    private final String repoListConfig;

    @Getter
    private List<RepositoryConfiguration> repositoryConfigurations;
    private final List<RepositoryConfiguration> deletedConfigurations = new ArrayList<>();

    private final PropertiesHolder properties;

    public RepositoryEditor(RepositoryFactoryProxy repositoryFactoryProxy, PropertiesHolder properties) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
        this.repoListConfig = repositoryFactoryProxy.getRepoListConfig();
        this.properties = properties;
        reload();
    }

    public static String getNewConfigName(List<RepositoryConfiguration> configurations, RepositoryMode repoMode) {
        var max = new AtomicInteger(0);
        var configName = repoMode.getId();
        var configNames = configurations.stream()
                .map(RepositoryConfiguration::getConfigName)
                .collect(Collectors.toSet());

        // existingConfigNames can contain ids that were deleted but were not saved, such ids should not be assigned to
        // a new repository
        var existingConfigNames = Props.getEnvironment().getProperty(configName + "-repository-configs");
        if (StringUtils.isNotEmpty(existingConfigNames)) {
            configNames.addAll(Arrays.asList(existingConfigNames.split(",")));
        }
        configNames.forEach(rc -> {
            if (rc.matches(configName + "\\d+")) {
                var num = rc.substring(configName.length());
                var i = Integer.parseInt(num);
                if (i > max.get()) {
                    max.set(i);
                }
            }
        });
        return configName + (max.incrementAndGet());
    }

    public Optional<RepositoryConfiguration> getRepositoryConfiguration(String id) {
        return repositoryConfigurations.stream()
            .filter(config -> config.getConfigName().equalsIgnoreCase(id))
            .findFirst();
    }

    public void reload() {
        repositoryConfigurations = new ArrayList<>();

        var repositoryConfigNames = split(properties.getProperty(repoListConfig));
        for (String configName : repositoryConfigNames) {
            if (isValidConfig(configName)) {
                var config = new RepositoryConfiguration(configName, properties);
                repositoryConfigurations.add(config);
            }
        }
    }

    private boolean isValidConfig(String configName) {
        return Objects.nonNull(
                properties.getPropertyResolver().getProperty(Comments.REPOSITORY_PREFIX + configName + ".factory"));
    }

    public void addRepository(RepositoryConfiguration configuration) {
        repositoryConfigurations.add(configuration);
    }

    public void deleteRepository(String configName) {
        Iterator<RepositoryConfiguration> it = repositoryConfigurations.iterator();
        while (it.hasNext()) {
            var config = it.next();
            if (config.getConfigName().equals(configName)) {
                deletedConfigurations.add(config);
                it.remove();
                break;
            }
        }
    }

    public void save() {
        for (RepositoryConfiguration config : deletedConfigurations) {
            config.revert();
        }

        deletedConfigurations.clear();

        String[] configNames = new String[repositoryConfigurations.size()];
        for (var i = 0; i < repositoryConfigurations.size(); i++) {
            var config = repositoryConfigurations.get(i);
            var newConfig = saveRepository(config);
            repositoryConfigurations.set(i, newConfig);
            configNames[i] = newConfig.getConfigName();
        }
        properties.setProperty(repoListConfig, String.join(",", configNames));
    }

    public void revertChanges() {
        for (RepositoryConfiguration configuration : deletedConfigurations) {
            configuration.revert();
        }
        deletedConfigurations.clear();

        for (RepositoryConfiguration configuration : repositoryConfigurations) {
            configuration.revert();
        }
        repositoryConfigurations.clear();

        properties.revertProperties(repoListConfig);
    }

    private RepositoryConfiguration saveRepository(RepositoryConfiguration config) {
        config.commit();
        if (config.isNameChangedIgnoreCase()) {
            var newConfigName = config.getName();
            properties.setProperty(Comments.REPOSITORY_PREFIX + config.getConfigName() + ".name", newConfigName);
        }

        return config;
    }

    public RepositoryConfiguration initializeConfiguration(RepositoryType type) {
        var repositoryMode = switch (repositoryFactoryProxy.getRepoListConfig()) {
            case AdministrationSettings.DESIGN_REPOSITORY_CONFIGS -> RepositoryMode.DESIGN;
            case AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS -> RepositoryMode.PRODUCTION;
            default -> throw new IllegalArgumentException("Unknown repository mode");
        };
        String configName = getNewConfigName(repositoryConfigurations, repositoryMode);
        return new RepositoryConfiguration(configName, properties, type.getFactoryId(), repositoryConfigurations, repositoryMode);
    }

    public void validate(RepositoryConfiguration config) throws RepositoryValidationException {
        RepositoryValidators.validate(config, repositoryConfigurations);
        RepositoryValidators.validateConnection(config);
    }

    private String[] split(String s) {
        return StringUtils.split(s, ',');
    }
}
