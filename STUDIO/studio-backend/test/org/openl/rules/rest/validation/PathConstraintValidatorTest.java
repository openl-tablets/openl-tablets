package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The location of a Git repository: a URL of an allowed scheme, or a local path of the server.
 *
 * <p>Each case is checked by a validator told which system the server runs on, whatever system runs the test.
 */
class PathConstraintValidatorTest {

    private record GitLocation(@PathConstraint(allowLeadingSlash = true, allowedSchemes = {"http", "https"}) String uri) {
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://github.com/openl-tablets/openl-tablets.git",
            "/openl/repositories/design",
            "openl/repositories/design"})
    void acceptsUrlsAndLocalPathsOnEverySystem(String uri) {
        assertEquals(List.of(), violations(uri, false));
        assertEquals(List.of(), violations(uri, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\openl\\repositories\\design", "C:/openl/repositories/design", "d:\\design"})
    void acceptsAWindowsPathOnWindows(String uri) {
        assertEquals(List.of(), violations(uri, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\openl\\repositories\\design", "C:/openl/repositories/design"})
    void refusesAWindowsPathOnAnotherSystem(String uri) {
        assertEquals(List.of("A Windows path names a local folder only when OpenL Studio runs on Windows."),
                violations(uri, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\openl\\repo?", "C:\\openl\\design\\"})
    void checksTheFoldersOfAWindowsPath(String uri) {
        assertEquals(1, violations(uri, true).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://host/design", "file:///openl/design"})
    void refusesASchemeNotAllowed(String uri) {
        var scheme = uri.substring(0, uri.indexOf(':'));

        var messages = violations(uri, true);

        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().startsWith("Scheme %s is not supported.".formatted(scheme)), messages::toString);
    }

    /** What a validator of a server on Windows, or on another system, says about the location. */
    private static List<String> violations(String uri, boolean windows) {
        var defaults = Validation.byDefaultProvider().configure().getDefaultConstraintValidatorFactory();
        try (var factory = Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new ConstraintValidatorFactory() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        return key == PathConstraintValidator.class ? (T) new PathConstraintValidator(windows)
                                : defaults.getInstance(key);
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {
                        defaults.releaseInstance(instance);
                    }
                })
                .buildValidatorFactory()) {
            return factory.getValidator().validate(new GitLocation(uri)).stream()
                    .map(ConstraintViolation::getMessage)
                    .toList();
        }
    }
}
