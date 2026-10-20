package org.openl.studio.audit.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.jndi.JndiPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.spring.env.ApplicationPropertySource;
import org.openl.spring.env.DefaultPropertySource;
import org.openl.spring.env.ServletContextPropertySource;
import org.openl.spring.env.SysEnvRefPropertySource;
import org.openl.spring.env.SysPropPropertySource;
import org.openl.studio.audit.model.AuditChanges;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.settings.converter.SettingDeclarations;
import org.openl.util.StringUtils;

/**
 * Announces the system settings the deployment changed between two starts of OpenL Studio: the ones environment
 * variables, system properties, init parameters of the context, JNDI or the properties files of the application set,
 * as the values of a Helm chart do.
 *
 * <p>Once a start has made the context of OpenL Studio, the settings the deployment sets are compared with the ones it
 * set at the last start, kept by the {@link SettingsSnapshotStore}, and the properties added, changed and removed
 * since are announced in one {@code settingsChanged} event marked as coming from the environment. The first start
 * records nothing. The deployment sets its settings once for a run of the server, so a refresh of the context, as a
 * save of the settings makes, compares nothing. A setting saved through the REST API is announced as it is saved and
 * takes no part here.
 *
 * <p>No value is kept or recorded anywhere: the snapshot holds fingerprints of the values
 * ({@link SettingFingerprints}), and the event names the properties only.
 *
 * <p>Only the settings OpenL Studio knows are compared: the ones it has defaults for, the ones its settings models
 * declare, those of every repository it is configured with, and those the last start kept. A value the default
 * already gives is no setting of the deployment.
 *
 * <p>A failure is logged without a value and never fails the start. The snapshot is then left as it was, so the next
 * start announces the change.
 */
@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EnvironmentSettingsAuditor {

    /** The kinds of the sources a deployment sets settings in; the defaults and the saved settings are none. */
    private static final Set<Class<?>> DEPLOYMENT_SOURCES = Set.of(SysPropPropertySource.class,
            SysEnvRefPropertySource.class, ServletContextPropertySource.class, JndiPropertySource.class,
            ApplicationPropertySource.class);
    private static final List<String> REPOSITORY_LISTS = List.of(AdministrationSettings.DESIGN_REPOSITORY_CONFIGS,
            AdministrationSettings.PRODUCTION_REPOSITORY_CONFIGS);
    /** The settings of a repository are named after it, as the repository of designs is by default. */
    private static final String DESIGN_REPOSITORY_PREFIX = Comments.REPOSITORY_PREFIX + "design";
    /** The detail that lists the settings the deployment set since the last start. */
    private static final String ADDED = "added";
    /** The detail that lists the settings the deployment no longer sets. */
    private static final String REMOVED = "removed";

    private final ConfigurableEnvironment environment;
    private final SettingsSnapshotStore store;
    private final AuditService auditService;
    private final SettingFingerprints fingerprints;

    /**
     * Fingerprints the secret settings by bcrypt, whatever password encoder OpenL Studio is configured with, so a
     * change of the encoder changes no fingerprint.
     */
    @Autowired
    public EnvironmentSettingsAuditor(ConfigurableEnvironment environment,
                                      SettingsSnapshotStore store,
                                      AuditService auditService) {
        this(environment, store, auditService, new SettingFingerprints(new BCryptPasswordEncoder()));
    }

    /**
     * Compares the settings the deployment sets with the ones of the last start, announces the changes and keeps the
     * settings of now for the next start. Nothing is compared while the audit log is off.
     */
    public void compareWithLastStart() {
        if (!auditService.isEnabled()) {
            return;
        }
        try {
            var previous = store.load();
            var last = previous.orElseGet(Map::of);
            var comparison = compare(deploymentValues(last.keySet()), last);
            if (previous.isPresent() && !comparison.kinds().isEmpty()) {
                announce(comparison.kinds());
            }
            if (!previous.equals(Optional.of(comparison.fingerprints()))) {
                store.save(comparison.fingerprints());
            }
        } catch (IOException | RuntimeException e) {
            // The exception names no value: neither a value nor a fingerprint reaches a message.
            log.warn("Failed to compare the settings of the deployment with the ones of its last start.", e);
        }
    }

    /**
     * The settings the deployment sets now, by property: the value its first source gives each known property, when
     * it is not the default of the property. A blank value counts as no value.
     *
     * <p>The properties the last start kept are known too: a repository removed since still has its settings the
     * deployment sets compared, rather than taken for removed.
     *
     * <p>A value the default already gives is no setting of the deployment: an environment variable that repeats a
     * default changes nothing, and one that goes back to the default is a setting removed.
     */
    private Map<String, String> deploymentValues(Set<String> keptProperties) {
        var sources = environment.getPropertySources().stream()
                .filter(source -> DEPLOYMENT_SOURCES.contains(source.getClass()))
                .toList();
        // The defaults of OpenL Studio, the openl-default.properties of its modules.
        var defaults = environment.getPropertySources().get(DefaultPropertySource.PROPS_NAME);
        var properties = knownProperties(defaults);
        properties.addAll(keptProperties);
        var values = new TreeMap<String, String>();
        properties.forEach(property -> valueOf(property, sources)
                .filter(value -> defaults == null || !value.equals(defaults.getProperty(property)))
                .ifPresent(value -> values.put(property, value)));
        return values;
    }

    private static Optional<String> valueOf(String property, List<PropertySource<?>> sources) {
        return sources.stream()
                .map(source -> source.getProperty(property))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(Predicate.not(String::isBlank))
                .findFirst();
    }

    /** The settings OpenL Studio knows: its defaults, the ones its models declare and those of its repositories. */
    private Set<String> knownProperties(@Nullable PropertySource<?> defaults) {
        var defaultProperties = defaults instanceof EnumerablePropertySource<?> enumerable
                ? List.of(enumerable.getPropertyNames())
                : List.<String>of();
        var properties = new HashSet<>(SettingDeclarations.names(setting -> true));
        properties.addAll(defaultProperties);
        var suffixes = new HashSet<>(SettingDeclarations.suffixes(setting -> true));
        defaultProperties.stream()
                .filter(property -> property.startsWith(DESIGN_REPOSITORY_PREFIX + '.'))
                .map(property -> property.substring(DESIGN_REPOSITORY_PREFIX.length()))
                .forEach(suffixes::add);
        repositories().forEach(repository -> suffixes
                .forEach(suffix -> properties.add(Comments.REPOSITORY_PREFIX + repository + suffix)));
        return properties;
    }

    /** The ids of the repositories OpenL Studio is configured with. */
    private Stream<String> repositories() {
        return REPOSITORY_LISTS.stream()
                .map(environment::getProperty)
                .filter(Objects::nonNull)
                .flatMap(list -> Arrays.stream(StringUtils.split(list, ',')));
    }

    /**
     * Compares the values of now with the fingerprints of the last start. A fingerprint of a value that stays is kept
     * unless it is made the old way, so only a new value is fingerprinted anew.
     */
    private Comparison compare(Map<String, String> values, Map<String, String> previous) {
        var now = new TreeMap<String, String>();
        var kinds = new TreeMap<String, String>();
        values.forEach((property, value) -> {
            var fingerprint = previous.get(property);
            var stays = fingerprint != null && fingerprints.matches(value, fingerprint);
            now.put(property, stays && fingerprints.isCurrent(property, fingerprint)
                    ? fingerprint
                    : fingerprints.of(property, value));
            if (!stays) {
                kinds.put(property, fingerprint == null ? ADDED : SettingsAuditor.CHANGED);
            }
        });
        AuditChanges.removed(previous.keySet(), values.keySet())
                .forEach(property -> kinds.put(property, REMOVED));
        return new Comparison(now, kinds);
    }

    private void announce(SortedMap<String, String> kinds) {
        var details = new LinkedHashMap<String, Object>();
        details.put(AuditEvent.ORIGIN, AuditEvent.ENVIRONMENT);
        details.putAll(namesByKind(kinds));
        auditService.recordEvent(SettingsAuditor.eventOf(kinds.keySet(), details));
    }

    /**
     * The details naming the settings added, changed and removed, without their values: the properties of each kind
     * of change in the order given, as {@code {"added": […], "changed": […], "removed": […]}}. A kind no property has
     * is left out.
     */
    private static Map<String, List<String>> namesByKind(Map<String, String> kinds) {
        // The names of the kinds sort as the details list them.
        return kinds.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, TreeMap::new,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
    }

    /**
     * What a start found: the fingerprints of the settings of now, and how each property added, changed or removed
     * since the last start changed, in the order of the properties.
     */
    private record Comparison(Map<String, String> fingerprints, SortedMap<String, String> kinds) {
    }
}
