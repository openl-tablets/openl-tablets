package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ProjectPomTest {

    @Test
    void resolvesARelativePathAsMavenDoes() {
        assertEquals("rules/pom.xml", ProjectPom.resolve("rules/Rating/pom.xml", "../pom.xml"));
        assertEquals("rules/pom.xml", ProjectPom.resolve("rules/Rating/pom.xml", ".."), "a folder names its pom.xml");
        assertEquals("parent/pom.xml", ProjectPom.resolve("rules/Rating/pom.xml", "..\\..\\parent\\pom.xml"));
        assertEquals("rules/Rating/build/parent.xml", ProjectPom.resolve("rules/Rating/pom.xml", "./build/parent.xml"));
        assertEquals("pom.xml", ProjectPom.resolve("Rating/pom.xml", "../"));
        assertNull(ProjectPom.resolve("Rating/pom.xml", "../../pom.xml"), "out of the root");
        assertEquals("/home/rules/pom.xml", ProjectPom.resolve("/home/rules/Rating/pom.xml", "../pom.xml"));
    }

    @Test
    void readsThePomsItNamesByARelativePath() throws IOException {
        var files = Map.of("rules/pom.xml", "<project>parent</project>");
        var pom = new ProjectPom("<project>child</project>", "rules/Rating/pom.xml", "design@1", files::get);

        var source = pom.modelSource();
        var parent = source.getRelatedSource("../pom.xml");

        assertEquals("<project>child</project>", new String(source.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
        assertNotNull(parent);
        assertEquals("rules/pom.xml", parent.getLocation());
        assertEquals("<project>parent</project>", new String(parent.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
        assertNull(source.getRelatedSource("../absent/pom.xml"));
        assertNull(source.getRelatedSource("../../../pom.xml"));
        assertNull(new ProjectPom("<project/>", "pom.xml", "x", path -> {
            throw new IOException("unreadable");
        }).modelSource().getRelatedSource("parent"), "Maven looks for the POM in the repository then");
        assertNotEquals(source.getLocationURI(), parent.getLocationURI());
    }

    @Test
    void isOneWithAnotherOfTheSameTextPathAndOrigin() {
        var pom = new ProjectPom("<project/>", "rules/pom.xml", "design@1", path -> null);

        assertEquals(pom, new ProjectPom("<project/>", "rules/pom.xml", "design@1", path -> "other reader"));
        assertEquals(pom.hashCode(),
                new ProjectPom("<project/>", "rules/pom.xml", "design@1", path -> null).hashCode());
        assertNotEquals(pom, new ProjectPom("<project/>", "rules/pom.xml", "design@2", path -> null));
        assertNotEquals(pom, ProjectPom.of("<project/>"));
        assertEquals("design@1:rules/pom.xml", pom.toString());
    }
}
