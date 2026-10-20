package org.openl.studio.settings.converter;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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
 * The system settings the settings models of OpenL Studio declare with {@link SettingPropertyName}.
 *
 * <p>A model names a setting on a field or a getter, by its property in full, or by the suffix a repository adds to
 * its own prefix, {@code repository.<id>}. The declarations are read once.
 *
 * <p>A model that cannot be read is skipped with a warning.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettingDeclarations {

    /** The packages the settings models are in. */
    private static final List<String> MODEL_PACKAGES = List.of("org.openl.rules.webstudio.web.admin",
            "org.openl.studio.settings");

    private static final List<SettingPropertyName> DECLARED = declared();

    /**
     * The properties the given declarations name in full.
     */
    public static Set<String> names(Predicate<SettingPropertyName> declarations) {
        return DECLARED.stream()
                .filter(declarations)
                .map(SettingPropertyName::value)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The suffixes the given declarations add to the prefix of a repository.
     */
    public static Set<String> suffixes(Predicate<SettingPropertyName> declarations) {
        return DECLARED.stream()
                .filter(declarations)
                .filter(setting -> setting.value().isBlank())
                .map(SettingPropertyName::suffix)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<SettingPropertyName> declared() {
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
                .flatMap(SettingDeclarations::declaredBy)
                .toList();
    }

    private static Stream<SettingPropertyName> declaredBy(BeanDefinition model) {
        try {
            var type = ClassUtils.resolveClassName(model.getBeanClassName(), SettingDeclarations.class.getClassLoader());
            return Stream.<AnnotatedElement>concat(Arrays.stream(type.getDeclaredFields()),
                            Arrays.stream(type.getDeclaredMethods()))
                    .flatMap(member -> Optional.ofNullable(member.getAnnotation(SettingPropertyName.class)).stream())
                    // Read here: a member of a type that cannot be loaded fails while the model is read.
                    .toList()
                    .stream();
        } catch (IllegalArgumentException | LinkageError e) {
            log.warn("Cannot read the settings of '{}'.", model.getBeanClassName(), e);
            return Stream.empty();
        }
    }
}
