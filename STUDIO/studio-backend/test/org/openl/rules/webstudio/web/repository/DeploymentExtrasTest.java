package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeploymentExtrasTest {

    @TempDir
    private Path temp;

    @Test
    void addsTheLibrariesAProjectDoesNotHoldToItsArchive() throws IOException {
        var archive = zip(Map.of("rules.xml", "<project/>", "lib/kept.jar", "own"));
        var kept = Files.writeString(temp.resolve("kept.jar"), "downloaded");
        var added = Files.writeString(temp.resolve("added.jar"), "downloaded");
        var extras = new DeploymentExtras(Map.of("Rules", List.of(kept, added)), Map.of());

        var entries = unzip(DeploymentExtras.withEntries(archive, extras.libraryEntries("Rules")));

        assertEquals(Map.of("rules.xml", "<project/>", "lib/kept.jar", "own", "lib/added.jar", "downloaded"), entries);
        assertSame(archive, DeploymentExtras.withEntries(archive, extras.libraryEntries("Other")));
    }

    @Test
    void archivesADownloadedProjectAsItWasUnpacked() throws IOException {
        var folder = Files.createDirectories(temp.resolve("domain-1.0.0-1/rules"));
        Files.writeString(folder.resolveSibling("rules.xml"), "<project/>");
        Files.writeString(folder.resolve("Main.xlsx"), "workbook");

        var entries = unzip(DeploymentExtras.archiveOf(folder.getParent()));

        assertEquals(Map.of("rules.xml", "<project/>", "rules/Main.xlsx", "workbook"), entries);
    }

    @Test
    void tellsWhetherItCarriesAnything() {
        assertTrue(DeploymentExtras.NONE.isEmpty());
        assertTrue(new DeploymentExtras(Map.of("Rules", List.of()), Map.of()).isEmpty());
        assertFalse(new DeploymentExtras(Map.of(), Map.of("Domain", temp)).isEmpty());
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static Map<String, String> unzip(byte[] archive) throws IOException {
        var entries = new LinkedHashMap<String, String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
