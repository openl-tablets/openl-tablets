package org.openl.rules.maven.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenLProjectScannerTest {

    @Test
    void findsRulesXmlFolders(@TempDir Path root) throws IOException {
        var auto = makeRulesProject(root.resolve("pricing/auto"));
        var eu = makeRulesProject(root.resolve("region/eu"));

        assertEquals(Set.of(auto, eu), normalize(OpenLPomlessParticipant.scan(root)));
    }

    @Test
    void skipsFoldersWithPomXml(@TempDir Path root) throws IOException {
        var classic = makeRulesProject(root.resolve("classic"));
        Files.writeString(classic.resolve("pom.xml"), "<project/>");
        var pomless = makeRulesProject(root.resolve("pomless"));

        assertEquals(Set.of(pomless), normalize(OpenLPomlessParticipant.scan(root)));
    }

    @Test
    void skipsTargetDirectory(@TempDir Path root) throws IOException {
        makeRulesProject(root.resolve("target/something"));
        var real = makeRulesProject(root.resolve("real"));

        assertEquals(Set.of(real), normalize(OpenLPomlessParticipant.scan(root)));
    }

    @Test
    void skipsHiddenDirectories(@TempDir Path root) throws IOException {
        makeRulesProject(root.resolve(".hidden"));
        var real = makeRulesProject(root.resolve("real"));

        assertEquals(Set.of(real), normalize(OpenLPomlessParticipant.scan(root)));
    }

    @Test
    void doesNotDescendIntoFoundProject(@TempDir Path root) throws IOException {
        var parent = makeRulesProject(root.resolve("parent"));
        makeRulesProject(parent.resolve("nested"));

        var found = OpenLPomlessParticipant.scan(root);
        assertEquals(1, found.size());
        assertTrue(found.get(0).endsWith("parent"));
    }

    private static Path makeRulesProject(Path folder) throws IOException {
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("rules.xml"), "<project/>");
        return folder.toAbsolutePath().normalize();
    }

    private static Set<Path> normalize(List<Path> paths) {
        return Set.copyOf(paths.stream().map(p -> p.toAbsolutePath().normalize()).toList());
    }
}
