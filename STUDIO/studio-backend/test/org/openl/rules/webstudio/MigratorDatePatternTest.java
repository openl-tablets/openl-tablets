package org.openl.rules.webstudio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies that an installation upgraded to 7.0.0 keeps writing the moment of a restored revision in its comments as
 * it wrote it before: the pattern of its removed date and time settings moves into the restore comment template.
 */
class MigratorDatePatternTest {

    private static final String KEY = "repository.design.comment-template.user-message.default.restored-from";

    @Test
    void readsThePatternTheRemovedSettingsGave() {
        assertNull(Migrator.legacyDateTimePattern(null, null, null), "Nothing set: the former default stays");
        assertEquals("yyyy-MM-dd HH:mm", Migrator.legacyDateTimePattern(null, null, "yyyy-MM-dd HH:mm"));
        // the date and the time set apart are joined as the default of the date and time did
        assertEquals("dd.MM.yyyy hh:mm:ss a", Migrator.legacyDateTimePattern("dd.MM.yyyy", null, null));
        assertEquals("MM/dd/yyyy HH:mm", Migrator.legacyDateTimePattern(null, "HH:mm", null));
        // the date and time composed of the other two is no pattern of its own
        assertEquals("dd.MM.yyyy HH:mm",
                Migrator.legacyDateTimePattern("dd.MM.yyyy", "HH:mm", "${data.format.date} ${data.format.time}"));
    }

    @Test
    void carriesOverOnlyAPatternTheCommentWrites() {
        // SimpleDateFormat reads a run of the day period letter as one letter, DateTimeFormatter refuses it
        assertEquals("MM/dd/yyyy hh:mm:ss a", Migrator.legacyDateTimePattern(null, "hh:mm:ss aa", null));
        // a zone, or u, a day of the week to SimpleDateFormat and a year to DateTimeFormatter, keeps the former default
        assertNull(Migrator.legacyDateTimePattern(null, null, "yyyy-MM-dd HH:mm z"));
        assertNull(Migrator.legacyDateTimePattern(null, null, "u, dd.MM.yyyy"));
    }

    @Test
    void namesThePatternInTheDatetimeOfATemplate() {
        var changed = Migrator.withDatePattern(Map.of(
                KEY, "Restored {revision} on {datetime}.",
                "repository.flat.comment-template.user-message.default.restored-from",
                "Restored on {datetime:" + Migrator.LEGACY_DATETIME_PATTERN + "}",
                "repository.other.comment-template.user-message.default.restored-from", "Restored {revision}",
                "repository.own.comment-template.user-message.default.restored-from", "On {datetime:yyyy}"),
                "dd.MM.yyyy HH:mm");

        assertEquals(Map.of(
                KEY, "Restored {revision} on {datetime:dd.MM.yyyy HH:mm}.",
                "repository.flat.comment-template.user-message.default.restored-from",
                "Restored on {datetime:dd.MM.yyyy HH:mm}"), changed,
                "A template without the moment, or with a pattern of its own, is left as it is");
    }

    @Test
    void writesTheFormerDefaultPatternWhereTheInstallationSetNone() {
        assertEquals(Map.of(KEY, "Restored on {datetime:MM/dd/yyyy hh:mm:ss a}."),
                Migrator.withDatePattern(Map.of(KEY, "Restored on {datetime}."), Migrator.LEGACY_DATETIME_PATTERN));
    }
}
