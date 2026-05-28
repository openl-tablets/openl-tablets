package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

class PomlessConverterTest {

    private static MavenProject project(Model model) {
        var p = new MavenProject(model);
        p.setOriginalModel(model);
        p.setFile(new File("/tmp/" + model.getArtifactId() + "/pom.xml"));
        return p;
    }

    private static Model openlModel(String artifactId) {
        var model = new Model();
        model.setModelVersion("4.0.0");
        model.setArtifactId(artifactId);
        model.setPackaging(OpenLPackagings.OPENL_PACKAGING);
        return model;
    }

    private static Plugin openlPlugin() {
        var plugin = new Plugin();
        plugin.setGroupId(OpenLPackagings.PLUGIN_GROUP_ID);
        plugin.setArtifactId(OpenLPackagings.PLUGIN_ARTIFACT_ID);
        plugin.setExtensions(true);
        return plugin;
    }

    private static Build buildWith(Plugin... plugins) {
        var build = new Build();
        for (var p : plugins) {
            build.addPlugin(p);
        }
        return build;
    }

    @Test
    void bareOpenLProjectIsDeletable() {
        var model = openlModel("clean");
        model.setBuild(buildWith(openlPlugin()));

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable(), "a bare openl project with only <extensions> must be deletable");
        assertTrue(plan.blockers().isEmpty());
        assertTrue(plan.hoistDeps().isEmpty());
        assertEquals("clean", plan.artifactId());
    }

    @Test
    void noBuildSectionIsDeletable() {
        var plan = PomlessConverter.analyze(project(openlModel("nobuild")));
        assertTrue(plan.deletable());
    }

    @Test
    void openLSiblingDependencyGoesToRulesXmlForExplicitGAV() {
        var model = openlModel("consumer");
        model.setBuild(buildWith(openlPlugin()));
        var sibling = new Dependency();
        sibling.setGroupId("com.example");
        sibling.setArtifactId("domain");
        sibling.setVersion("1.0.0");
        sibling.setType(OpenLPackagings.ZIP_DEPENDENCY_TYPE);
        model.addDependency(sibling);

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable());
        assertTrue(plan.hoistDeps().isEmpty(), "OpenL siblings are never hoisted to the anchor");
        assertEquals(1, plan.rulesXmlDeps().size(),
                "the OpenL sibling dep moves into rules.xml so its GAV is explicit alongside <name>");
        var moved = plan.rulesXmlDeps().get(0);
        assertEquals("domain", moved.getArtifactId());
        assertEquals(OpenLPackagings.ZIP_DEPENDENCY_TYPE, moved.getType(),
                "type=zip is preserved so writeRulesXmlDeps emits the canonical 3-seg coordinate (g:a:v)");
    }

    @Test
    void providedTileDependencyIsHoistedToAnchor() {
        var model = openlModel("withlib");
        model.setBuild(buildWith(openlPlugin()));
        var lib = new Dependency();
        lib.setGroupId("com.eisgroup");
        lib.setArtifactId("message-bundle");
        lib.setType("tile");
        lib.setScope("provided");
        model.addDependency(lib);

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable(), "a provided java dep doesn't block deletion");
        assertEquals(1, plan.hoistDeps().size(), "provided/tile deps go to the anchor");
        assertEquals("message-bundle", plan.hoistDeps().get(0).getArtifactId());
        assertTrue(plan.rulesXmlDeps().isEmpty(), "provided/tile deps must not go to rules.xml (not packaged)");
    }

    @Test
    void packagedJarDependencyGoesToRulesXmlNotAnchor() {
        var model = openlModel("withjar");
        model.setBuild(buildWith(openlPlugin()));
        var jar = new Dependency();
        jar.setGroupId("com.eisgroup");
        jar.setArtifactId("rating-details-store");
        jar.setVersion("1.2.3");
        jar.setOptional("true");
        // no <type> (default jar), no <scope> (default compile) → packaged into lib/
        model.addDependency(jar);

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable());
        assertTrue(plan.hoistDeps().isEmpty(), "a packaged jar must NOT be hoisted to the anchor");
        assertEquals(1, plan.rulesXmlDeps().size(), "a compile/default jar goes into the project's rules.xml");
        assertEquals("rating-details-store", plan.rulesXmlDeps().get(0).getArtifactId());
    }

    @Test
    void providedJarStaysOnAnchorNotRulesXml() {
        var model = openlModel("withprovidedjar");
        model.setBuild(buildWith(openlPlugin()));
        var jar = new Dependency();
        jar.setGroupId("com.eisgroup");
        jar.setArtifactId("service-discovery-api");
        jar.setVersion("1.0");
        jar.setScope("provided"); // provided jar → NOT packaged → anchor, not rules.xml
        model.addDependency(jar);

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.rulesXmlDeps().isEmpty(), "provided jar is not packaged → not in rules.xml");
        assertEquals(1, plan.hoistDeps().size(), "provided jar goes to the anchor");
    }

    @Test
    void dependencyWithExclusionsBlocksConversion() {
        var model = openlModel("with-excl");
        model.setBuild(buildWith(openlPlugin()));
        var jar = new Dependency();
        jar.setGroupId("com.example");
        jar.setArtifactId("library");
        jar.setVersion("1.0");
        var excl = new Exclusion();
        excl.setGroupId("transitive");
        excl.setArtifactId("dep");
        jar.addExclusion(excl);
        model.addDependency(jar);

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable(), "a dep with <exclusions> can't be expressed in rules.xml → blocked");
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("<exclusions>")),
                "the blocker reason must mention <exclusions> and the offending dep");
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("com.example:library")));
    }

    @Test
    void planCarriesTheProjectGroupId() {
        var model = openlModel("with-group");
        model.setGroupId("com.cardif.openl");

        var plan = PomlessConverter.analyze(project(model));

        assertEquals("com.cardif.openl", plan.groupId(),
                "the migrator's flattenGroupId heuristic compares the leaf's original groupId against the anchor's");
    }

    @Test
    void managedJarVersionIsBackfilledFromEffectiveModel() {
        // Raw pom (originalModel): a jar dep whose <version> is supplied by a parent/BOM — omitted here.
        var raw = openlModel("managed");
        raw.setBuild(buildWith(openlPlugin()));
        var rawDep = new Dependency();
        rawDep.setGroupId("org.apache.commons");
        rawDep.setArtifactId("commons-text");
        raw.addDependency(rawDep); // no <version> — inherited via dependencyManagement

        // Effective model: Maven's dependency-management injection has filled the version in.
        var effective = openlModel("managed");
        var effDep = new Dependency();
        effDep.setGroupId("org.apache.commons");
        effDep.setArtifactId("commons-text");
        effDep.setVersion("1.13.0");
        effective.addDependency(effDep);

        var project = new MavenProject(effective);
        project.setOriginalModel(raw);
        project.setFile(new File("/tmp/managed/pom.xml"));

        var plan = PomlessConverter.analyze(project);

        assertEquals(1, plan.rulesXmlDeps().size());
        assertEquals("1.13.0", plan.rulesXmlDeps().get(0).getVersion(),
                "a version-less (managed) jar dep must be backfilled from the effective model, never left null");
    }

    @Test
    void propertyPlaceholderVersionIsReplacedByTheEffectiveValue() {
        // Raw model: a sibling dep whose <version> is the literal '${revision}' — the corporate
        // pattern at cidp-openl. Without the fix this lands verbatim in the mavenArtifact coord
        // and (a) corrupts rules.xml and (b) breaks Matcher.appendReplacement with "No group with name {version}".
        var raw = openlModel("propful");
        raw.setBuild(buildWith(openlPlugin()));
        var rawDep = new Dependency();
        rawDep.setGroupId("com.example.integration");
        rawDep.setArtifactId("rules-shared");
        rawDep.setVersion("${revision}"); // raw declaration
        rawDep.setType(OpenLPackagings.ZIP_DEPENDENCY_TYPE);
        raw.addDependency(rawDep);

        var effective = openlModel("propful");
        var effDep = new Dependency();
        effDep.setGroupId("com.example.integration");
        effDep.setArtifactId("rules-shared");
        effDep.setVersion("3.5-SNAPSHOT"); // post-interpolation
        effDep.setType(OpenLPackagings.ZIP_DEPENDENCY_TYPE);
        effective.addDependency(effDep);

        var project = new MavenProject(effective);
        project.setOriginalModel(raw);
        project.setFile(new File("/tmp/propful/pom.xml"));

        var plan = PomlessConverter.analyze(project);

        assertEquals(1, plan.rulesXmlDeps().size());
        assertEquals("3.5-SNAPSHOT", plan.rulesXmlDeps().get(0).getVersion(),
                "the interpolated version must win over the raw '${revision}' so rules.xml carries the real value");
    }

    private static Xpp3Dom config(String name, String value) {
        var c = new Xpp3Dom("configuration");
        var child = new Xpp3Dom(name);
        child.setValue(value);
        c.addChild(child);
        return c;
    }

    @Test
    void nonThresholdPluginConfigurationBlocks() {
        var model = openlModel("configured");
        var plugin = openlPlugin();
        plugin.setConfiguration(config("skipITs", "true"));
        model.setBuild(buildWith(plugin));

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable());
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("skipITs")));
    }

    @Test
    void thresholdOnlyConfigurationIsConvertibleAndRecorded() {
        var model = openlModel("thr");
        var plugin = openlPlugin();
        plugin.setConfiguration(config("dependenciesThreshold", "4"));
        model.setBuild(buildWith(plugin));

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable(), "a dependenciesThreshold-only config must NOT block conversion");
        assertEquals(Integer.valueOf(4), plan.dependenciesThreshold());
    }

    @Test
    void deploymentPackageConfigurationIsIgnoredNotBlocking() {
        var model = openlModel("deploypkg");
        var plugin = openlPlugin();
        plugin.setConfiguration(config("deploymentPackage", "true"));
        model.setBuild(buildWith(plugin));

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable(), "deploymentPackage is a deprecated no-op (auto-discovered) — must not block");
        assertTrue(plan.blockers().isEmpty());
        assertEquals(null, plan.dependenciesThreshold());
    }

    @Test
    void deploymentPackageAndThresholdTogetherAreConvertible() {
        var model = openlModel("combo");
        var plugin = openlPlugin();
        var cfg = config("deploymentPackage", "true");
        var t = new Xpp3Dom("dependenciesThreshold");
        t.setValue("4");
        cfg.addChild(t);
        plugin.setConfiguration(cfg);
        model.setBuild(buildWith(plugin));

        var plan = PomlessConverter.analyze(project(model));

        assertTrue(plan.deletable(), "deploymentPackage (ignored) + dependenciesThreshold (hoisted) → convertible");
        assertEquals(Integer.valueOf(4), plan.dependenciesThreshold());
    }

    @Test
    void thresholdRecordedEvenWhenOtherConfigBlocks() {
        var model = openlModel("thrblock");
        var plugin = openlPlugin();
        var cfg = config("dependenciesThreshold", "5");
        var skip = new Xpp3Dom("skipITs");
        skip.setValue("true");
        cfg.addChild(skip);
        plugin.setConfiguration(cfg);
        model.setBuild(buildWith(plugin));

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable(), "skipITs still blocks");
        assertEquals(Integer.valueOf(5), plan.dependenciesThreshold(),
                "the threshold is still recorded so the anchor can take the max");
    }

    @Test
    void pluginExecutionsBlock() {
        var model = openlModel("executed");
        var plugin = openlPlugin();
        var exec = new PluginExecution();
        exec.setId("extra");
        exec.addGoal("generate");
        plugin.addExecution(exec);
        model.setBuild(buildWith(plugin));

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable());
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("<executions>")));
    }

    @Test
    void extraPluginBlocks() {
        var model = openlModel("shaded");
        var shade = new Plugin();
        shade.setGroupId("org.apache.maven.plugins");
        shade.setArtifactId("maven-shade-plugin");
        model.setBuild(buildWith(openlPlugin(), shade));

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable());
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("maven-shade-plugin")));
    }

    @Test
    void profilesBlock() {
        var model = openlModel("profiled");
        model.setBuild(buildWith(openlPlugin()));
        var profile = new Profile();
        profile.setId("ci");
        model.addProfile(profile);

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable());
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("<profiles>")));
    }

    @Test
    void projectPropertiesBlock() {
        var model = openlModel("propful");
        model.setBuild(buildWith(openlPlugin()));
        model.addProperty("foo", "bar");

        var plan = PomlessConverter.analyze(project(model));

        assertFalse(plan.deletable());
        assertTrue(plan.blockers().stream().anyMatch(b -> b.contains("<properties>")));
    }
}
