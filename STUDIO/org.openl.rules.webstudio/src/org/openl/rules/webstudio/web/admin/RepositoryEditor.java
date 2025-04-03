package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openl.config.PropertiesHolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.repository.RepositoryFactoryProxy;
import org.openl.util.StringUtils;

public class RepositoryEditor {

    private final RepositoryFactoryProxy repositoryFactoryProxy;
    private final String repoListConfig;

    private List<RepositoryConfiguration> repositoryConfigurations;
    private final List<RepositoryConfiguration> deletedConfigurations = new ArrayList<>();
    private final Set<String> forbiddenIds = new HashSet<>();

    private final PropertiesHolder properties;

    public RepositoryEditor(RepositoryFactoryProxy repositoryFactoryProxy, PropertiesHolder properties) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
        this.repoListConfig = repositoryFactoryProxy.getRepoListConfig();
        this.properties = properties;
        reload();
    }

    public void setForbiddenIds(String... ids) {
        forbiddenIds.clear();
        if (ids != null) {
            forbiddenIds.addAll(Arrays.stream(ids).filter(Objects::nonNull).collect(Collectors.toSet()));
        }
    }

    public static String getNewConfigName(List<RepositoryConfiguration> configurations, RepositoryMode repoMode) {
        AtomicInteger max = new AtomicInteger(0);
        String configName = repoMode.getId();
        Set<String> configNames = configurations.stream()
                .map(RepositoryConfiguration::getConfigName)
                .collect(Collectors.toSet());

        // existingConfigNames can contain ids that were deleted but were not saved, such ids should not be assigned to
        // a new repository
        String existingConfigNames = Props.getEnvironment().getProperty(configName + "-repository-configs");
        if (StringUtils.isNotEmpty(existingConfigNames)) {
            configNames.addAll(Arrays.asList(existingConfigNames.split(",")));
        }
        configNames.forEach(rc -> {
            if (rc.matches(configName + "\\d+")) {
                String num = rc.substring(configName.length());
                int i = Integer.parseInt(num);
                if (i > max.get()) {
                    max.set(i);
                }
            }
        });
        return configName + (max.incrementAndGet());
    }

    public List<RepositoryConfiguration> getRepositoryConfigurations() {
        return repositoryConfigurations;
    }

    public Optional<RepositoryConfiguration> getRepositoryConfiguration(String id) {
        return repositoryConfigurations.stream()
            .filter(config -> config.getConfigName().equalsIgnoreCase(id))
            .findFirst();
    }

    public void reload() {
        repositoryConfigurations = new ArrayList<>();

        String[] repositoryConfigNames = split(properties.getProperty(repoListConfig));
        for (String configName : repositoryConfigNames) {
            if (isValidConfig(configName)) {
                RepositoryConfiguration config = new RepositoryConfiguration(configName, properties);
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
        deleteRepository(configName, null);
    }

    public void deleteRepository(String configName, Callback callback) {
        Iterator<RepositoryConfiguration> it = repositoryConfigurations.iterator();
        while (it.hasNext()) {
            RepositoryConfiguration config = it.next();
            if (config.getConfigName().equals(configName)) {
                deletedConfigurations.add(config);
                it.remove();

                if (callback != null) {
                    callback.onDelete(configName);
                }

                break;
            }
        }
    }

    public void validate() throws RepositoryValidationException {
        for (RepositoryConfiguration config : repositoryConfigurations) {
            if (forbiddenIds.contains(config.getConfigName())) {
                String msg = String.format("Repository name '%s' already exists. Please, choose another name.",
                        config.getName());
                throw new RepositoryValidationException(msg);
            }
            RepositoryValidators.validate(config, repositoryConfigurations);
            RepositoryValidators.validateConnection(config, repositoryFactoryProxy);
        }
    }

    public void save() {
        save(null);
    }

    public void save(Callback callback) {
        for (RepositoryConfiguration config : deletedConfigurations) {
            if (callback != null) {
                callback.onDelete(config.getConfigName());
            }
            config.revert();
        }

        deletedConfigurations.clear();

        String[] configNames = new String[repositoryConfigurations.size()];
        for (int i = 0; i < repositoryConfigurations.size(); i++) {
            RepositoryConfiguration config = repositoryConfigurations.get(i);
            RepositoryConfiguration newConfig = saveRepository(config);
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
            String newConfigName = config.getName();
            properties.setProperty(Comments.REPOSITORY_PREFIX + config.getConfigName() + ".name", newConfigName);
        }

        return config;
    }

    public RepositoryConfiguration initializeConfiguration(RepositoryType type) {
        RepositoryMode repositoryMode = switch (repositoryFactoryProxy.getRepoListConfig()) {
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

    public abstract static class Callback {
        public void onDelete(String configName) {
            // Do nothing
        }
    }
}
