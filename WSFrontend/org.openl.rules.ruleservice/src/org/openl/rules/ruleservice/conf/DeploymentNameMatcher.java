package org.openl.rules.ruleservice.conf;

import java.util.regex.Pattern;

import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The matcher provides ability to split deployments between several applications by deployment name
 *
 * @author Vladyslav Pikus
 * @since 5.21.4
 */
final class DeploymentNameMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DeploymentNameMatcher.class);
    private static final Pattern WILDCARD_REDUNDANT_OCCUR = Pattern.compile("\\*{2,}");
    public static final DeploymentNameMatcher DEFAULT = new DeploymentNameMatcher();

    private final Pattern pattern;

    public DeploymentNameMatcher() {
        this(null);
    }

    public DeploymentNameMatcher(String source) {
        if (source == null) {
            this.pattern = null;
        } else {
            this.pattern = compilePattern(source);
        }
    }

    /**
     * Compiles RegEx pattern from string
     * <p>
     * Examples:
     * <pre>
     *  compilePattern("") = null
     *  compilePattern("*") = null
     *  compilePattern("foo") = foo
     *  compilePattern("foo-*") = \Qfoo-\E.*
     *  compilePattern("*-foo-*") = .*\Q-foo-\E.*
     *  compilePattern("foo, bar-*") = (\Qfoo\E)|(\Qbar-\E.*)
     *  compilePattern("foo-**") = null
     * </pre>
     *
     * @param source name patterns with coma separator
     * @return compiled RegEx pattern, {@code null} if source is empty or doesn't contain valid patterns
     */
    private Pattern compilePattern(String source) {
        String[] patterns = source.split(",");
        Character delimiter = null;
        StringBuilder regex = new StringBuilder();
        for (String pattern : patterns) {
            if (isBlankPattern(pattern)) {
                continue;
            }
            if (WILDCARD_REDUNDANT_OCCUR.matcher(pattern).find()) {
                LOG.warn("Rule name pattern '{}' cannot have more than one wildcard letter in a row", pattern);
            } else {
                if (delimiter != null) {
                    regex.append(delimiter);
                } else {
                    delimiter = '|';
                }
                regex.append("(\\Q").append(pattern.trim()).append("\\E)");
            }
        }

        return regex.length() > 0 ? Pattern.compile(regex.toString().replace("*", "\\E.*\\Q")) : null;
    }

    private boolean isBlankPattern(String pattern) {
        return StringUtils.isBlank(pattern) || "*".equals(pattern);
    }

    public boolean hasMatches(String deploymentName) {
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(deploymentName).matches();
    }

}
