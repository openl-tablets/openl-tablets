package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;
import org.openl.rules.repository.RepositoryMode;

/**
 * The settings of a Git repository, as OpenL Studio stores and reverts them.
 *
 * @author Yury Molchan
 */
class GitRepositorySettingsTest {

    private static final String PREFIX = "repository.design";
    private static final String CONNECTION_TIMEOUT = PREFIX + ".connection-timeout";
    private static final String FAILED_AUTHENTICATION_SECONDS = PREFIX + ".failed-authentication-seconds";
    private static final String MAX_AUTHENTICATION_ATTEMPTS = PREFIX + ".max-authentication-attempts";

    private static InMemoryProperties properties(Map<String, Object> stored) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("stored", new HashMap<>(stored)));
        return new InMemoryProperties(environment);
    }

    /** A user message template is as long as a comment at most: a longer one could only give comments too long. */
    @Test
    void refusesAUserMessageTemplateLongerThanAComment() {
        var settings = new GitRepositorySettings(properties(Map.of()), PREFIX, RepositoryMode.DESIGN);
        settings.setInvalidCommentMessage("Invalid comment");
        settings.setDefaultCommentSave("a".repeat(255));
        settings.setDefaultCommentCreate("a".repeat(256));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var templates = factory.getValidator()
                    .validate(settings, RepositorySettings.Validation.Design.class)
                    .stream()
                    .filter(violation -> violation.getPropertyPath().toString().startsWith("defaultComment"))
                    .toList();

            assertEquals(1, templates.size());
            assertEquals("defaultCommentCreate", templates.getFirst().getPropertyPath().toString());
            assertEquals("The user message template can have at most 255 characters.",
                    templates.getFirst().getMessage());
        }
    }

    @Test
    void revertsTheConnectionTimeoutAndTheAuthenticationLimits() {
        var target = properties(Map.of(CONNECTION_TIMEOUT, "60",
                FAILED_AUTHENTICATION_SECONDS, "300",
                MAX_AUTHENTICATION_ATTEMPTS, "3"));
        var settings = new GitRepositorySettings(target, PREFIX, RepositoryMode.DESIGN);
        settings.setConnectionTimeout(5);
        settings.setFailedAuthenticationSeconds(10);
        settings.setMaxAuthenticationAttempts(1);
        settings.store(target);

        settings.revert(target);

        assertEquals(60, settings.getConnectionTimeout());
        assertEquals(300, settings.getFailedAuthenticationSeconds());
        assertEquals(3, settings.getMaxAuthenticationAttempts());
        assertNull(target.getConfig().get(CONNECTION_TIMEOUT));
        assertNull(target.getConfig().get(FAILED_AUTHENTICATION_SECONDS));
        assertNull(target.getConfig().get(MAX_AUTHENTICATION_ATTEMPTS));
    }
}
