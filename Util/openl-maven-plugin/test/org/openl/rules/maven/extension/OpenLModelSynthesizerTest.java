package org.openl.rules.maven.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

class OpenLModelSynthesizerTest {

    @Test
    void minimalDescriptorProducesValidModel() {
        var coords = new OpenLCoordinates("com.example.pricing", "auto");
        var desc = new ProjectDescriptor();
        desc.setName("Auto Rules");

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), null, null);

        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("com.example.pricing", model.getGroupId());
        assertEquals("auto", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("openl", model.getPackaging());
        assertEquals("Auto Rules", model.getName());
        assertNull(model.getParent(), "no parent must be wired when no anchor is supplied");
        assertNull(model.getBuild(), "synthesised pom must not declare a build section");
    }

    @Test
    void anchorBecomesParentForBuildInheritance() {
        var coords = new OpenLCoordinates("com.example.pricing", "auto");
        var desc = new ProjectDescriptor();

        var anchorModel = new Model();
        anchorModel.setGroupId("com.example");
        anchorModel.setArtifactId("anchor");
        anchorModel.setVersion("2.0.0");
        var anchor = new MavenProject(anchorModel);

        var model = OpenLModelSynthesizer.synthesize(coords, "2.0.0", desc, Map.of(), null, anchor);

        assertNotNull(model.getParent(),
                "anchor must be wired as parent so the pom-less project inherits anchor's <build>");
        assertEquals("com.example", model.getParent().getGroupId());
        assertEquals("anchor", model.getParent().getArtifactId());
        assertEquals("2.0.0", model.getParent().getVersion());
        assertEquals("", model.getParent().getRelativePath(),
                "empty <relativePath/> must force reactor/repo lookup — anchor is always in-session");
    }

    @Test
    void explicitMavenArtifactBecomesDependency() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setName("Domain");
        dep.setMavenArtifact("com.other:domain:2.0.0");
        desc.setDependencies(List.of(dep));

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), null, null);

        assertEquals(1, model.getDependencies().size());
        var maven = model.getDependencies().get(0);
        assertEquals("com.other", maven.getGroupId());
        assertEquals("domain", maven.getArtifactId());
        assertEquals("2.0.0", maven.getVersion());
        assertEquals("zip", maven.getType());
        assertNull(maven.getOptional(),
                "default zip-type (OpenL sibling) must not be optional — consumers must inherit the sibling");
    }

    @Test
    void mavenArtifactSupportsTypeAndClassifier() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        // Aether format: groupId:artifactId:extension:classifier:version (version last).
        dep.setMavenArtifact("com.other:domain:jar:tests:2.0.0");
        desc.setDependencies(List.of(dep));

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), null, null);

        var maven = model.getDependencies().get(0);
        assertEquals("2.0.0", maven.getVersion());
        assertEquals("jar", maven.getType());
        assertEquals("tests", maven.getClassifier());
        assertEquals("true", maven.getOptional(),
                "jar-type deps are self-contained inside the OpenL project's lib/; optional=true stops downstream consumers from inheriting them");
    }

    @Test
    void jarMavenArtifactIsMarkedOptional() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        // Aether format: groupId:artifactId:extension:version (version last).
        dep.setMavenArtifact("org.apache.commons:commons-text:jar:1.13.1");
        desc.setDependencies(List.of(dep));

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), null, null);

        var maven = model.getDependencies().get(0);
        assertEquals("jar", maven.getType());
        assertEquals("1.13.1", maven.getVersion(), "jar version must be the literal from mavenArtifact, not projectVersion");
        assertEquals("true", maven.getOptional(),
                "jar-type mavenArtifact must be installed as optional so downstream OpenL projects don't inherit it transitively");
    }

    @Test
    void nameFallbackResolvesToSiblingCoordinates() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setName("Domain");
        desc.setDependencies(List.of(dep));
        var domainCoords = new OpenLCoordinates("com.example", "domain");

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc,
                Map.of("Domain", domainCoords), null, null);

        assertEquals(1, model.getDependencies().size());
        var maven = model.getDependencies().get(0);
        assertEquals("com.example", maven.getGroupId());
        assertEquals("domain", maven.getArtifactId());
        assertEquals("zip", maven.getType());
        assertEquals("1.0.0", maven.getVersion(),
                "name-resolved dep must use the literal project version so the installed pom holds a concrete version");
    }

    @Test
    void unresolvedNameProducesNoDependency() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setName("Missing");
        desc.setDependencies(List.of(dep));

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), null, null);

        assertNotNull(model.getDependencies());
        assertEquals(0, model.getDependencies().size());
    }

    @Test
    void mavenArtifactWinsOverNameFallback() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setName("Domain");
        dep.setMavenArtifact("com.external:remote-domain:9.9.9");
        desc.setDependencies(List.of(dep));
        var domainCoords = new OpenLCoordinates("com.example", "domain");

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc,
                Map.of("Domain", domainCoords), null, null);

        var maven = model.getDependencies().get(0);
        assertEquals("com.external", maven.getGroupId());
        assertEquals("remote-domain", maven.getArtifactId());
        assertEquals("9.9.9", maven.getVersion());
    }

    @Test
    void anchorDependencyManagementOverridesMavenArtifactVersion() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setMavenArtifact("com.other:domain:1.0.0");
        desc.setDependencies(List.of(dep));

        var managed = managementEntry("com.other", "domain", "2.0.0");
        var dm = Map.of("com.other:domain", managed);

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), dm, null);

        var maven = model.getDependencies().get(0);
        assertEquals("2.0.0", maven.getVersion(),
                "anchor's <dependencyManagement> should override the version declared in <mavenArtifact>");
        assertEquals("zip", maven.getType(), "type from <mavenArtifact> stays as-is");
    }

    @Test
    void anchorDependencyManagementOverridesNameResolvedVersion() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setName("Domain");
        desc.setDependencies(List.of(dep));
        var domainCoords = new OpenLCoordinates("com.example", "domain");

        var managed = managementEntry("com.example", "domain", "9.9.9");
        var dm = Map.of("com.example:domain", managed);

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc,
                Map.of("Domain", domainCoords), dm, null);

        assertEquals("9.9.9", model.getDependencies().get(0).getVersion(),
                "management applies uniformly — name-resolved deps too");
    }

    @Test
    void unmanagedDependencyKeepsItsExplicitVersion() {
        var coords = new OpenLCoordinates("com.example", "consumer");
        var desc = new ProjectDescriptor();
        var dep = new ProjectDependencyDescriptor();
        dep.setMavenArtifact("com.other:domain:1.0.0");
        desc.setDependencies(List.of(dep));

        // dependencyManagement exists but doesn't cover this GA
        var other = managementEntry("com.unrelated", "thing", "5.0.0");
        var dm = Map.of("com.unrelated:thing", other);

        var model = OpenLModelSynthesizer.synthesize(coords, "1.0.0", desc, Map.of(), dm, null);

        assertEquals("1.0.0", model.getDependencies().get(0).getVersion(),
                "without a matching management entry the version from <mavenArtifact> wins");
    }

    private static Dependency managementEntry(String groupId, String artifactId, String version) {
        var d = new Dependency();
        d.setGroupId(groupId);
        d.setArtifactId(artifactId);
        d.setVersion(version);
        return d;
    }
}
