package org.openl.itest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Restricts a test to CI runners that provide Docker.
 *
 * <p>The {@code CI} environment variable, set to {@code true} by common CI systems (GitHub Actions,
 * GitLab CI, CircleCI, and others), enables the test. A local build leaves it unset and therefore skips
 * the test. The {@code noDocker} guard additionally disables the test on CI legs without Docker
 * (Windows, macOS).
 *
 * <p>Place this directly on each test class. JUnit resolves the conditions through this
 * meta-annotation, but does not inherit them from a superclass.
 *
 * @author Yury Molchan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@DisabledIfSystemProperty(named = "noDocker", matches = ".*")
@interface CiWithDocker {
}
