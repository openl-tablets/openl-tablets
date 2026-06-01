package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenLPackagingsTest {

    // ---- parseMavenArtifact --------------------------------------------------------------------

    @Test
    void parsesThreeSegmentCoordinateAsZip() {
        var dep = OpenLPackagings.parseMavenArtifact("com.example:domain:1.0.0");
        assertEquals("com.example", dep.getGroupId());
        assertEquals("domain", dep.getArtifactId());
        assertEquals("1.0.0", dep.getVersion());
        assertEquals("zip", dep.getType(), "a 3-seg coordinate defaults to the OpenL zip type");
        assertNull(dep.getClassifier());
    }

    @Test
    void parsesFourSegmentCoordinateWithExplicitType() {
        var dep = OpenLPackagings.parseMavenArtifact("com.example:lib:jar:1.0.0");
        assertEquals("jar", dep.getType());
        assertEquals("1.0.0", dep.getVersion());
        assertNull(dep.getClassifier());
    }

    @Test
    void parsesFiveSegmentCoordinateWithClassifier() {
        var dep = OpenLPackagings.parseMavenArtifact("com.example:lib:jar:tests:1.0.0");
        assertEquals("jar", dep.getType());
        assertEquals("tests", dep.getClassifier());
        assertEquals("1.0.0", dep.getVersion(), "the version is always the last segment");
    }

    @Test
    void trimsSurroundingWhitespace() {
        var dep = OpenLPackagings.parseMavenArtifact("  com.example:domain:1.0.0  ");
        assertEquals("com.example", dep.getGroupId());
        assertEquals("1.0.0", dep.getVersion());
    }

    @Test
    void rejectsCoordinatesOutsideThreeToFiveSegments() {
        assertNull(OpenLPackagings.parseMavenArtifact("com.example:domain"), "2 segments is too few");
        assertNull(OpenLPackagings.parseMavenArtifact("a:b:c:d:e:f"), "6 segments is too many");
    }

    @Test
    void rejectsNullOrBlankCoordinate() {
        assertNull(OpenLPackagings.parseMavenArtifact(null));
        assertNull(OpenLPackagings.parseMavenArtifact(""));
        assertNull(OpenLPackagings.parseMavenArtifact("   "));
    }

    // ---- isOpenL / dependencyType / ga / plugin coordinates ------------------------------------

    @Test
    void recognisesOpenLPackagings() {
        assertTrue(OpenLPackagings.isOpenL("openl"));
        assertTrue(OpenLPackagings.isOpenL("openl-jar"));
        assertFalse(OpenLPackagings.isOpenL("jar"));
        assertFalse(OpenLPackagings.isOpenL("pom"));
    }

    @Test
    void mapsPackagingToDependencyType() {
        assertEquals("zip", OpenLPackagings.dependencyType("openl"));
        assertEquals("jar", OpenLPackagings.dependencyType("openl-jar"));
        assertEquals("zip", OpenLPackagings.dependencyType("anything-else"),
                "non-OpenL packaging falls back to the zip default");
    }

    @Test
    void recognisesPluginCoordinates() {
        assertTrue(OpenLPackagings.isOpenLPlugin("org.openl.rules", "openl-maven-plugin"));
        assertFalse(OpenLPackagings.isOpenLPlugin("org.openl.rules", "other"));
        assertTrue(OpenLPackagings.isFlattenPlugin("org.codehaus.mojo", "flatten-maven-plugin"));
        assertFalse(OpenLPackagings.isFlattenPlugin("org.openl.rules", "openl-maven-plugin"));
    }

    @Test
    void buildsGroupArtifactKey() {
        assertEquals("com.example:domain", OpenLPackagings.ga("com.example", "domain"));
    }

    // ---- reactorOpenLVersions ------------------------------------------------------------------

    @Test
    void indexesOnlyOpenLProjectsByGroupArtifact() {
        var index = OpenLPackagings.reactorOpenLVersions(List.of(
                project("com.example", "rules", "1.0", "openl"),
                project("com.example", "lib", "2.0", "openl-jar"),
                project("com.example", "plain", "3.0", "jar")));

        assertEquals("1.0", index.get("com.example:rules"));
        assertEquals("2.0", index.get("com.example:lib"));
        assertNull(index.get("com.example:plain"), "non-OpenL projects are not indexed");
    }

    // ---- materialiseInstallPom -----------------------------------------------------------------

    @Test
    void installPomStripsParentBuildAndFlattenSkip(@TempDir Path dir) throws IOException {
        var model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("com.example");
        model.setArtifactId("rules");
        model.setVersion("1.0");
        model.setPackaging("openl");
        var parent = new Parent();
        parent.setGroupId("com.example");
        parent.setArtifactId("anchor");
        parent.setVersion("1.0");
        model.setParent(parent);
        model.setBuild(new Build());
        model.addProperty(OpenLPackagings.FLATTEN_SKIP_PROPERTY, "true");
        model.addProperty("keep.me", "value");

        var pom = OpenLPackagings.materialiseInstallPom(model, dir);

        assertEquals(OpenLPackagings.INSTALL_POM_FILE_NAME, pom.getFileName().toString());
        var xml = Files.readString(pom);
        assertFalse(xml.contains("<parent>"), "the install pom must be flat — no <parent>");
        assertFalse(xml.contains("<build"), "the bootstrap <build> stub must be stripped");
        assertFalse(xml.contains("flatten.skip"), "the transient flatten.skip property must be stripped");
        assertTrue(xml.contains("keep.me"), "unrelated properties must survive");
        assertTrue(xml.contains("<artifactId>rules</artifactId>"));
    }

    private static MavenProject project(String groupId, String artifactId, String version, String packaging) {
        var model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setPackaging(packaging);
        return new MavenProject(model);
    }
}
