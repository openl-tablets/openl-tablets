package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies that an installation upgraded to 7.1.0 drops the comment templates and the Customize comments flag of its
 * design repositories, and keeps validating the comments exactly as it did.
 */
class MigratorCommentTemplatesTest {

    @Test
    void removesTheTemplatesAndTheFlagOfEveryRepository() {
        var stored = Map.of(
                "repository.design.comment-template.user-message.default.save", "Save {project-name}",
                "repository.design.comment-template.user-message.default.create", "Create {project-name}",
                "repository.flat.comment-template.user-message.default.copied-from", "Copy {project-name}",
                "repository.flat.comment-template.user-message.default.restored-from", "Restore {revision}",
                "repository.flat.comment-template.use-custom-comments", "false");

        var changes = migrate(stored);

        assertEquals(stored.size(), changes.size());
        changes.forEach((name, value) -> assertNull(value, name));
    }

    @Test
    void emptiesAPatternThatWasNotApplied() {
        var stored = Map.of(
                "repository.off.comment-template.use-custom-comments", "false",
                "repository.off.comment-template.comment-validation-pattern", "EPBDS-.+",
                // without the flag, the repository did not customize comments either
                "repository.unset.comment-template.comment-validation-pattern", "EPBDS-.+");

        var changes = migrate(stored);

        assertEquals("", changes.get("repository.off.comment-template.comment-validation-pattern"));
        assertEquals("", changes.get("repository.unset.comment-template.comment-validation-pattern"));
    }

    @Test
    void keepsTheValidationOfARepositoryThatCustomizedComments() {
        var stored = Map.of(
                "repository.on.comment-template.use-custom-comments", " TRUE ",
                "repository.on.comment-template.comment-validation-pattern", "EPBDS-.+",
                "repository.on.comment-template.invalid-comment-message", "Start with a ticket",
                "repository.blank.comment-template.comment-validation-pattern", "",
                "repository.design.comment-template.$ref", "repo-default.design.comment-template",
                // the defaults are not stored settings of an installation
                "repo-default.design.comment-template.user-message.default.save", "Save {project-name}");

        var changes = migrate(stored);

        // only the flag goes: the pattern it applied now applies by itself
        assertEquals(Map.of("repository.on.comment-template.use-custom-comments", "-"), withDashForNull(changes));
    }

    private static Map<String, String> migrate(Map<String, String> stored) {
        return Migrator.commentSettingsTo710(stored.keySet().toArray(String[]::new), stored::get);
    }

    private static Map<String, String> withDashForNull(Map<String, String> changes) {
        var shown = new HashMap<String, String>();
        changes.forEach((name, value) -> shown.put(name, value == null ? "-" : value));
        return shown;
    }
}
