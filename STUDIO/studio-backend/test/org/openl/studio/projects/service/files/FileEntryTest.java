package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class FileEntryTest {

    private static FileEntry entry(String fullPath, String data) {
        return new FileEntry(fullPath, data.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void entriesHoldingEqualBytesAreEqual() {
        var one = entry("rules/Main.xlsx", "content");
        var other = entry("rules/Main.xlsx", "content");

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void entriesDifferingInPathOrBytesAreNotEqual() {
        var entry = entry("rules/Main.xlsx", "content");

        assertNotEquals(entry, entry("rules/Other.xlsx", "content"));
        assertNotEquals(entry, entry("rules/Main.xlsx", "other"));
        assertNotEquals(entry, new Object());
    }

    @Test
    void toStringReportsTheSizeInsteadOfTheBytes() {
        assertEquals("FileEntry[fullPath=rules/Main.xlsx, data=7 bytes]",
                entry("rules/Main.xlsx", "content").toString());
        assertEquals("FileEntry[fullPath=rules/Main.xlsx, data=0 bytes]",
                new FileEntry("rules/Main.xlsx", null).toString());
    }
}
