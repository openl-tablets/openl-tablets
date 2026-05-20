package org.openl.rules.maven.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.junit.jupiter.api.Test;

/**
 * Covers neutralising an inherited flatten-maven-plugin on pom-less projects: they have no on-disk pom for
 * flatten to read, so it's skipped via {@code flatten.skip} (a switch that only exists since 1.6.0, hence the
 * version bump). OpenL's prepare-pom does the actual flattening.
 */
class OpenLPomlessParticipantTest {

    private static Plugin flattenPlugin(String version) {
        var plugin = new Plugin();
        plugin.setGroupId("org.codehaus.mojo");
        plugin.setArtifactId("flatten-maven-plugin");
        plugin.setVersion(version);
        return plugin;
    }

    private static Plugin openlPlugin() {
        var plugin = new Plugin();
        plugin.setGroupId("org.openl.rules");
        plugin.setArtifactId("openl-maven-plugin");
        return plugin;
    }

    private static Plugin pluginIn(Model model, String artifactId) {
        if (model.getBuild() == null) {
            return null;
        }
        for (var p : model.getBuild().getPlugins()) {
            if (artifactId.equals(p.getArtifactId())) {
                return p;
            }
        }
        return null;
    }

    // ---- findOpenLPluginInBuildPlugins ---------------------------------------------------------

    @Test
    void findsOpenLPluginDeclaredInBuildPlugins() {
        var model = new Model();
        var build = new Build();
        build.addPlugin(openlPlugin());
        model.setBuild(build);

        assertNotNull(OpenLPomlessParticipant.findOpenLPluginInBuildPlugins(model));
    }

    @Test
    void ignoresOpenLPluginDeclaredOnlyInPluginManagement() {
        var model = new Model();
        var build = new Build();
        var mgmt = new PluginManagement();
        mgmt.addPlugin(openlPlugin());
        build.setPluginManagement(mgmt);
        model.setBuild(build);

        assertNull(OpenLPomlessParticipant.findOpenLPluginInBuildPlugins(model),
                "a pluginManagement-only entry never executes, so it must not act as an anchor");
    }

    @Test
    void returnsNullWhenOpenLPluginAbsentOrNoBuild() {
        var withBuild = new Model();
        withBuild.setBuild(new Build());
        assertNull(OpenLPomlessParticipant.findOpenLPluginInBuildPlugins(withBuild), "no openl plugin declared");
        assertNull(OpenLPomlessParticipant.findOpenLPluginInBuildPlugins(new Model()), "no <build> at all");
    }

    // ---- findFlattenPlugin ---------------------------------------------------------------------

    @Test
    void findsFlattenDeclaredInBuildPlugins() {
        var model = new Model();
        var build = new Build();
        build.addPlugin(flattenPlugin("1.4.1"));
        model.setBuild(build);

        var found = OpenLPomlessParticipant.findFlattenPlugin(model);
        assertNotNull(found);
        assertEquals("1.4.1", found.getVersion());
    }

    @Test
    void findsFlattenDeclaredInPluginManagement() {
        var model = new Model();
        var build = new Build();
        var mgmt = new PluginManagement();
        mgmt.addPlugin(flattenPlugin("1.4.1"));
        build.setPluginManagement(mgmt);
        model.setBuild(build);

        assertNotNull(OpenLPomlessParticipant.findFlattenPlugin(model));
    }

    @Test
    void returnsNullWhenFlattenAbsentOrNoBuild() {
        var withBuild = new Model();
        withBuild.setBuild(new Build());
        assertNull(OpenLPomlessParticipant.findFlattenPlugin(withBuild), "no flatten declared");
        assertNull(OpenLPomlessParticipant.findFlattenPlugin(new Model()), "no <build> at all");
    }

    // ---- needsVersionBump ----------------------------------------------------------------------

    @Test
    void bumpsVersionsOlderThan160() {
        assertTrue(OpenLPomlessParticipant.needsVersionBump("1.4.1"));
        assertTrue(OpenLPomlessParticipant.needsVersionBump("1.5.9"));
        assertTrue(OpenLPomlessParticipant.needsVersionBump(null));
        assertTrue(OpenLPomlessParticipant.needsVersionBump(""));
    }

    @Test
    void keepsVersions160AndNewer() {
        assertFalse(OpenLPomlessParticipant.needsVersionBump("1.6.0"));
        assertFalse(OpenLPomlessParticipant.needsVersionBump("1.7.0"));
        assertFalse(OpenLPomlessParticipant.needsVersionBump("2.0.0"));
    }

    // ---- decorateForModelBuilder ---------------------------------------------------------------

    @Test
    void oldInheritedFlattenIsBumpedTo160AndSkipped() {
        var model = new Model();

        OpenLPomlessParticipant.decorateForModelBuilder(model, "6.0.0", flattenPlugin("1.4.1"));

        assertEquals("true", model.getProperties().getProperty("flatten.skip"),
                "flatten.skip must be set so the goal short-circuits");
        var flatten = pluginIn(model, "flatten-maven-plugin");
        assertNotNull(flatten, "an explicit flatten entry must override the inherited 1.4.1 version");
        assertEquals("1.6.0", flatten.getVersion());
        assertNotNull(pluginIn(model, "openl-maven-plugin"), "the openl extension stub must remain");
    }

    @Test
    void newEnoughInheritedFlattenIsSkippedWithoutBump() {
        var model = new Model();

        OpenLPomlessParticipant.decorateForModelBuilder(model, "6.0.0", flattenPlugin("1.6.0"));

        assertEquals("true", model.getProperties().getProperty("flatten.skip"));
        assertNull(pluginIn(model, "flatten-maven-plugin"),
                "1.6.0 already honours flatten.skip — no version-override entry needed");
    }

    @Test
    void noInheritedFlattenLeavesModelAlone() {
        var model = new Model();

        OpenLPomlessParticipant.decorateForModelBuilder(model, "6.0.0", null);

        assertNull(model.getProperties().getProperty("flatten.skip"));
        assertNull(pluginIn(model, "flatten-maven-plugin"));
        assertNotNull(pluginIn(model, "openl-maven-plugin"));
    }
}
