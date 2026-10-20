package org.openl.studio.settings.converter;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

/**
 * Tells the system settings whose values are secret, such as passwords and access keys.
 *
 * <p>A setting is secret when a settings model declares it so: a field or a getter annotated with
 * {@link SettingPropertyName#secret()}, naming the property in full or by the suffix a repository adds to its own
 * prefix. The declarations are read once, from the settings models of OpenL Studio.
 *
 * <p>A property is taken for secret by its name as well, whether a model declares it or not: when the last part of
 * the name holds {@code password} or {@code secret}, or ends with {@code key}. A secret value is never shown, so
 * taking one setting too many for secret costs nothing.
 *
 * <p>The value of an ordinary setting may still hold credentials, such as an address with a user and a token in it:
 * {@link #masked(String)} hides them.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecretSettings {

    /** The packages the settings models are in. */
    private static final List<String> MODEL_PACKAGES = List.of("org.openl.rules.webstudio.web.admin",
            "org.openl.studio.settings");

    /** The user and the password of an address, as in {@code https://user:token@host}. */
    private static final Pattern ADDRESS_CREDENTIALS = Pattern.compile("(?<=://)[^/?#@\\s]+@");
    /** The user and the password of a connection string, as in {@code jdbc:oracle:thin:user/password@host}. */
    private static final Pattern CONNECTION_CREDENTIALS = Pattern.compile("(?<=:)[^:/@\\s]+/[^@\\s]+@");
    /**
     * A parameter of an address or a connection string whose name tells a secret, as {@code ?password=} does. The
     * name is looked through once, so a long value without the parameter costs no more than reading it.
     */
    private static final Pattern SECRET_PARAMETERS = Pattern.compile(
            "(?i)([?&;](?=[^=&;?#]*(?:pass|pwd|secret|token|key|credential))[^=&;?#]*+=)[^&;#]*");
    /** The signature of a shared access address, as {@code ?sig=} of Azure Blob Storage. */
    private static final Pattern SIGNATURE_PARAMETER = Pattern.compile("(?i)([?&;]sig=)[^&;#]*");
    private static final String HIDDEN = "***";

    private static final List<SettingPropertyName> DECLARED = declaredSecrets();
    private static final Set<String> NAMES = DECLARED.stream()
            .map(SettingPropertyName::value)
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> SUFFIXES = DECLARED.stream()
            .filter(setting -> setting.value().isBlank())
            .map(SettingPropertyName::suffix)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * Whether the value of the setting of the given property is secret.
     */
    public static boolean isSecret(String property) {
        return NAMES.contains(property) || SUFFIXES.stream().anyMatch(property::endsWith) || isSecretByName(property);
    }

    /**
     * The value of a setting with the credentials it holds hidden: the user and the password of an address or a
     * connection string, and every parameter whose name tells a secret, such as a password, a token or a key. Any
     * other part of the value is kept.
     */
    public static String masked(String value) {
        var hidden = ADDRESS_CREDENTIALS.matcher(value).replaceAll(HIDDEN + "@");
        hidden = CONNECTION_CREDENTIALS.matcher(hidden).replaceAll(HIDDEN + "@");
        hidden = SIGNATURE_PARAMETER.matcher(hidden).replaceAll("$1" + HIDDEN);
        return SECRET_PARAMETERS.matcher(hidden).replaceAll("$1" + HIDDEN);
    }

    private static boolean isSecretByName(String property) {
        var name = property.substring(property.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return name.contains("password") || name.contains("secret") || name.endsWith("key");
    }

    /** The settings the models declare secret. */
    private static List<SettingPropertyName> declaredSecrets() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                // An abstract model declares settings for its subclasses.
                return true;
            }
        };
        scanner.addIncludeFilter((reader, factory) -> true);
        return MODEL_PACKAGES.stream()
                .flatMap(modelPackage -> scanner.findCandidateComponents(modelPackage).stream())
                .flatMap(SecretSettings::declaredSecretsOf)
                .toList();
    }

    /** The settings a model declares secret; none for a model that cannot be read, which names keep secret still. */
    private static Stream<SettingPropertyName> declaredSecretsOf(BeanDefinition model) {
        try {
            var type = ClassUtils.resolveClassName(model.getBeanClassName(), SecretSettings.class.getClassLoader());
            return Stream.<AnnotatedElement>concat(Arrays.stream(type.getDeclaredFields()),
                            Arrays.stream(type.getDeclaredMethods()))
                    .flatMap(member -> Optional.ofNullable(member.getAnnotation(SettingPropertyName.class)).stream())
                    .filter(SettingPropertyName::secret)
                    // Read here: a member of a type that cannot be loaded fails while the model is read.
                    .toList()
                    .stream();
        } catch (IllegalArgumentException | LinkageError e) {
            log.warn("Cannot read the secret settings of '{}'.", model.getBeanClassName(), e);
            return Stream.empty();
        }
    }
}
