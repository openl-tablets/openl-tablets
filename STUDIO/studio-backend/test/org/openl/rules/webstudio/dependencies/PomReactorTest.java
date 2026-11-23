package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

class PomReactorTest {

    private static final String VERSION = "<version>2.0.0-SNAPSHOT</version>";

    /** The root of the build: a group of modules and a module in a folder of its own. */
    private static final String ROOT = """
            <project>
                <groupId>com.example</groupId>
                <artifactId>build</artifactId>
                <version>2.0.0-SNAPSHOT</version>
                <modules>
                    <module>group</module>
                    <module>integration/census/</module>
                    <module>gone</module>
                </modules>
            </project>
            """;

    private static final String GROUP = """
            <project>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>build</artifactId>
                    <version>2.0.0-SNAPSHOT</version>
                </parent>
                <artifactId>group</artifactId>
                <modules>
                    <module>rules</module>
                    <module>broken</module>
                </modules>
            </project>
            """;

    private static final Map<String, String> BUILD = Map.of(
            "build/pom.xml", ROOT,
            "build/group/pom.xml", GROUP,
            "build/integration/census/pom.xml", module("build", "census", VERSION),
            "build/group/broken/pom.xml", "<project>");

    @Test
    void takesEveryModuleOfTheTopmostParentBesideIt() {
        var root = PomReactor.rootOf(pom(module("group", "rules", VERSION), BUILD));

        assertEquals("build/pom.xml", root.path());
        var modules = PomReactor.modulesOf(root);
        assertEquals(Set.of("com.example:group:2.0.0-SNAPSHOT", "com.example:census:2.0.0-SNAPSHOT",
                "com.example:rules:2.0.0-SNAPSHOT"), modules);
        assertTrue(PomReactor.holds(modules, artifact("census", "2.0.0-SNAPSHOT")), "a module of another group");
        assertFalse(PomReactor.holds(modules, artifact("census", "1.0.0")), "another version is downloaded");
        assertFalse(PomReactor.holds(modules, artifact("absent", "2.0.0-SNAPSHOT")), "no module is that one");
    }

    /** A CI-friendly version is put in from the properties of the build, as Maven reads it. */
    @Test
    void readsTheVersionsOfTheModulesWithThePropertiesOfTheBuild() {
        var revision = "<version>${revision}</version>";
        var root = ROOT.replace("<version>2.0.0-SNAPSHOT</version>",
                revision + "<properties><revision>2.0.0-SNAPSHOT</revision></properties>");
        var group = GROUP.replace("<version>2.0.0-SNAPSHOT</version>", revision);
        var build = Map.of("build/pom.xml", root,
                "build/group/pom.xml", group,
                "build/integration/census/pom.xml", module("build", "census", revision));

        var modules = PomReactor.modulesOf(PomReactor.rootOf(pom(module("group", "rules", revision), build)));

        assertTrue(PomReactor.holds(modules, artifact("census", "2.0.0-SNAPSHOT")), modules.toString());
        assertTrue(PomReactor.holds(modules, artifact("rules", "2.0.0-SNAPSHOT")), modules.toString());
    }

    @Test
    void hasNoBuildWithoutAParentBesideItListingModules() {
        assertNull(PomReactor.rootOf(ProjectPom.of("<project/>")));
        assertNull(PomReactor.rootOf(pom(module("group", "rules", VERSION + "<relativePath/>"), BUILD)),
                "an empty relativePath reads the parent from the repository");
        assertNull(PomReactor.rootOf(pom(module("group", "rules", "<version>1.0.0</version>"), BUILD)),
                "the pom.xml beside it is another parent");
        assertNull(PomReactor.rootOf(pom("<project>", BUILD)), "a pom.xml that cannot be read");
        assertNull(PomReactor.rootOf(pom(module("group", "rules", VERSION),
                Map.of("build/group/pom.xml", module("build", "group", VERSION)))), "a parent listing no module");
    }

    /** A module: its parent, with the version and the rest the parent tag holds, and its artifact. */
    private static String module(String parent, String artifactId, String parentVersion) {
        return """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>%s</artifactId>
                        %s
                    </parent>
                    <artifactId>%s</artifactId>
                </project>
                """.formatted(parent, parentVersion, artifactId);
    }

    private static ProjectPom pom(String text, Map<String, String> files) {
        var all = new HashMap<>(files);
        all.put("build/group/rules/pom.xml", text);
        return new ProjectPom(text, "build/group/rules/pom.xml", "test", all::get);
    }

    private static DefaultArtifact artifact(String artifactId, String version) {
        return new DefaultArtifact("com.example", artifactId, "zip", version);
    }
}
