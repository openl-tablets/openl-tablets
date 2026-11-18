package org.openl.rules.maven;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * A goal that follows the build when it skips tests.
 * <p>
 * The tests are skipped by either Maven property: {@code skipTests} or {@code maven.test.skip}. The {@code compile}
 * goal then leaves the modules of the {@code tests} folder out of compilation, and the {@code test} and
 * {@code verify} goals do not run.
 */
abstract class TestsSkippingMojo extends BaseOpenLMojo {

    /**
     * Parameter to skip the OpenL Tablets tests if it set to 'true'.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    /**
     * Same as {@link #skipTests}, set by the {@code maven.test.skip} property, which Maven uses to skip both the
     * compilation and the run of the tests.
     *
     * @since 7.1.0
     */
    @Parameter(property = "maven.test.skip")
    private boolean mavenTestSkip;

    /**
     * Tells whether the build skips tests.
     */
    boolean isTestsSkipped() {
        return skipTests || mavenTestSkip;
    }
}
