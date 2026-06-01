package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Unit coverage for {@link PomlessMojo}'s pure planning logic: the anchor-resolution rules
 * ({@link PomlessMojo#collapseAnchorOf}, {@link PomlessMojo#isPassThrough},
 * {@link PomlessMojo#inferFlattenGroupId}, {@link PomlessMojo#proposeSubAnchors}), the coordinate form
 * ({@link PomlessMojo#mavenArtifactCoords}), the {@code rules.xml} dependency merge
 * ({@link PomlessMojo#writeRulesXmlDeps}), and the anchor-pom edits
 * ({@link PomlessMojo#ensurePluginConfigured}, {@link PomlessMojo#removeModules}).
 */
class PomlessMojoTest {

    private static MavenProject project(String packaging) {
        var model = new Model();
        model.setPackaging(packaging);
        return new MavenProject(model);
    }

    private static Model passThroughModel() {
        var model = new Model();
        model.setPackaging("pom");
        // <modules> alone is allowed — pass-throughs typically only carry parent + GAV + modules.
        model.addModule("child");
        return model;
    }

    // ---- collapseAnchorOf -----------------------------------------------------------------------

    @Test
    void anchorIsTheNearestNonOpenLAncestorWhenNoPassThroughsCollapse() {
        var root = project("pom");
        var rating = project("pom");
        var leaf = project(OpenLPackagings.OPENL_PACKAGING);
        var reactor = Map.of(
                Path.of("/repo"), root,
                Path.of("/repo/rating"), rating,
                Path.of("/repo/rating/leaf"), leaf);
        var collapsed = new HashSet<Path>();

        var anchor = PomlessMojo.collapseAnchorOf(
                Path.of("/repo/rating/leaf"), reactor, Set.of(), Path.of("/repo"), collapsed);

        assertSame(rating, anchor, "rating is non-pass-through (per the empty passThroughDirs) — anchor stops there");
        assertTrue(collapsed.isEmpty(), "nothing was collapsed");
    }

    @Test
    void anchorIsTheRootWhenTheOpenLProjectIsADirectChild() {
        var root = project("pom");
        var leaf = project(OpenLPackagings.OPENL_PACKAGING);
        var reactor = Map.of(
                Path.of("/repo"), root,
                Path.of("/repo/leaf"), leaf);

        assertSame(root, PomlessMojo.collapseAnchorOf(
                Path.of("/repo/leaf"), reactor, Set.of(), Path.of("/repo"), new HashSet<>()));
    }

    @Test
    void collapseWalksThroughPassThroughAggregatorsAndCollectsThem() {
        // cidp-openl-style: every intermediate is pass-through; collapse jumps straight to ${project}.
        var root = project("pom");
        var lookups = project("pom");
        var colombia = project("pom");
        var sbk = project("pom");
        var leaf = project(OpenLPackagings.OPENL_PACKAGING);
        var reactor = Map.of(
                Path.of("/repo"), root,
                Path.of("/repo/lookups"), lookups,
                Path.of("/repo/lookups/colombia"), colombia,
                Path.of("/repo/lookups/colombia/sbk"), sbk,
                Path.of("/repo/lookups/colombia/sbk/leaf"), leaf);
        var passThroughs = Set.of(
                Path.of("/repo/lookups"),
                Path.of("/repo/lookups/colombia"),
                Path.of("/repo/lookups/colombia/sbk"));
        var collapsed = new HashSet<Path>();

        var anchor = PomlessMojo.collapseAnchorOf(
                Path.of("/repo/lookups/colombia/sbk/leaf"), reactor, passThroughs, Path.of("/repo"), collapsed);

        assertSame(root, anchor, "all intermediates are pass-through → anchor collapses to ${project}");
        assertTrue(collapsed.containsAll(passThroughs), "every pass-through walked through must be deletion-flagged");
    }

    @Test
    void collapseStopsAtTheFirstNonPassThroughAncestor() {
        // Mixed: rating has real content (not pass-through) → stop there even though deeper intermediates collapse.
        var root = project("pom");
        var rating = project("pom"); // non-pass-through (not in passThroughDirs)
        var sub = project("pom"); // pass-through
        var leaf = project(OpenLPackagings.OPENL_PACKAGING);
        var reactor = Map.of(
                Path.of("/repo"), root,
                Path.of("/repo/rating"), rating,
                Path.of("/repo/rating/sub"), sub,
                Path.of("/repo/rating/sub/leaf"), leaf);
        var passThroughs = Set.of(Path.of("/repo/rating/sub"));
        var collapsed = new HashSet<Path>();

        var anchor = PomlessMojo.collapseAnchorOf(
                Path.of("/repo/rating/sub/leaf"), reactor, passThroughs, Path.of("/repo"), collapsed);

        assertSame(rating, anchor, "rating is non-pass-through — anchor stops there, not at ${project}");
        assertTrue(collapsed.contains(Path.of("/repo/rating/sub")), "the pass-through between rating and leaf collapses");
    }

    @Test
    void invocationRootIsAlwaysAnAnchorEvenWhenPassThrough() {
        // ${project} itself can be pass-through — we still anchor there (can't go above the invocation root).
        var root = project("pom");
        var leaf = project(OpenLPackagings.OPENL_PACKAGING);
        var reactor = Map.of(
                Path.of("/repo"), root,
                Path.of("/repo/leaf"), leaf);

        assertSame(root, PomlessMojo.collapseAnchorOf(
                Path.of("/repo/leaf"), reactor, Set.of(Path.of("/repo")), Path.of("/repo"), new HashSet<>()));
    }

    @Test
    void anchorIsNullWhenNoAncestorReactorPomExists() {
        var leaf = project(OpenLPackagings.OPENL_PACKAGING);
        var reactor = Map.of(Path.of("/repo/leaf"), leaf);

        assertNull(PomlessMojo.collapseAnchorOf(
                Path.of("/repo/leaf"), reactor, Set.of(), Path.of("/somewhere/else"), new HashSet<>()));
    }

    // ---- isPassThrough --------------------------------------------------------------------------

    @Test
    void aggregatorWithOnlyParentAndModulesIsPassThrough() {
        assertTrue(PomlessMojo.isPassThrough(passThroughModel()));
    }

    @Test
    void onlyPomPackagingCanBePassThrough() {
        var jar = new Model();
        jar.setPackaging("jar");
        assertFalse(PomlessMojo.isPassThrough(jar), "a non-pom artefact is never pass-through");
        var openl = new Model();
        openl.setPackaging(OpenLPackagings.OPENL_PACKAGING);
        assertFalse(PomlessMojo.isPassThrough(openl));
    }

    @Test
    void buildPluginsOrPluginManagementMakesItNonPassThrough() {
        var model = passThroughModel();
        var build = new Build();
        build.addPlugin(new Plugin());
        model.setBuild(build);
        assertFalse(PomlessMojo.isPassThrough(model));
    }

    @Test
    void profilesPropertiesOrDependenciesMakeItNonPassThrough() {
        var withProfile = passThroughModel();
        var profile = new Profile();
        profile.setId("ci");
        withProfile.addProfile(profile);
        assertFalse(PomlessMojo.isPassThrough(withProfile), "profiles are real config");

        var withProperty = passThroughModel();
        withProperty.addProperty("foo", "bar");
        assertFalse(PomlessMojo.isPassThrough(withProperty), "properties affect interpolation");

        var withDep = passThroughModel();
        var dep = new Dependency();
        dep.setGroupId("g");
        dep.setArtifactId("a");
        dep.setVersion("1");
        withDep.addDependency(dep);
        assertFalse(PomlessMojo.isPassThrough(withDep));
    }

    // ---- inferFlattenGroupId --------------------------------------------------------------------

    private static MavenProject anchorAt(String groupId, Path basedir) {
        var model = new Model();
        model.setPackaging("pom");
        model.setGroupId(groupId);
        var p = new MavenProject(model);
        p.setFile(basedir.resolve("pom.xml").toFile());
        return p;
    }

    private static PomlessConverter.Plan leafPlan(String groupId, Path leafFolder) {
        return new PomlessConverter.Plan("leaf", groupId, leafFolder.resolve("pom.xml"),
                true, List.of(), List.of(), List.of(), null);
    }

    @Test
    void flattenWhenAllLeavesShareTheAnchorGroupId() {
        // cidp-openl style: every leaf inherits the same corporate groupId; flat-derivation preserves it.
        var anchor = anchorAt("com.cardif.openl", Path.of("/repo"));
        var leaves = List.of(
                leafPlan("com.cardif.openl", Path.of("/repo/lookups/colombia/sbk/leaf")),
                leafPlan("com.cardif.openl", Path.of("/repo/lookups/mexico/coppel/leaf")));

        assertTrue(PomlessMojo.inferFlattenGroupId(anchor, leaves),
                "all leaves match the anchor groupId verbatim → flattenGroupId preserves coordinates");
    }

    @Test
    void keepDefaultWhenLeavesUsePathDerivedGroupIds() {
        // A repo where each leaf has a path-encoded groupId — the Maven-default derivation already matches,
        // so the heuristic must NOT add flattenGroupId.
        var anchor = anchorAt("com.example", Path.of("/repo"));
        var leaves = List.of(
                leafPlan("com.example.lookups.colombia.sbk", Path.of("/repo/lookups/colombia/sbk/leaf")),
                leafPlan("com.example.lookups.colombia.tuya", Path.of("/repo/lookups/colombia/tuya/leaf")));

        assertFalse(PomlessMojo.inferFlattenGroupId(anchor, leaves),
                "path-derived groupIds already match the default — no need to flip the flag");
    }

    @Test
    void tieDefaultsToFalseWhenLeavesAreDirectChildren() {
        // For direct children the flat and path-derived forms are identical → tie → keep the default off
        // (no need to clutter the anchor config with redundant flattenGroupId).
        var anchor = anchorAt("com.example", Path.of("/repo"));
        var leaves = List.of(
                leafPlan("com.example", Path.of("/repo/leaf-a")),
                leafPlan("com.example", Path.of("/repo/leaf-b")));

        assertFalse(PomlessMojo.inferFlattenGroupId(anchor, leaves));
    }

    @Test
    void majorityWins() {
        // 2 leaves match flat, 1 matches the path form → flat majority wins.
        var anchor = anchorAt("com.example", Path.of("/repo"));
        var leaves = List.of(
                leafPlan("com.example", Path.of("/repo/lookups/colombia/sbk/leaf")),
                leafPlan("com.example", Path.of("/repo/lookups/mexico/coppel/leaf")),
                leafPlan("com.example.lookups.poland.bp", Path.of("/repo/lookups/poland/bp/leaf")));

        assertTrue(PomlessMojo.inferFlattenGroupId(anchor, leaves));
    }

    @Test
    void noFlattenWhenAnchorHasNoGroupId() {
        var anchor = new MavenProject(new Model());
        anchor.setFile(new File("/repo/pom.xml"));
        var leaves = List.of(leafPlan("com.example", Path.of("/repo/leaf")));

        assertFalse(PomlessMojo.inferFlattenGroupId(anchor, leaves));
    }

    // ---- mavenArtifactCoords -------------------------------------------------------------------

    private static Dependency dep(String groupId, String artifactId, String version, String type, String classifier) {
        var d = new Dependency();
        d.setGroupId(groupId);
        d.setArtifactId(artifactId);
        d.setVersion(version);
        d.setType(type);
        d.setClassifier(classifier);
        return d;
    }

    @Test
    void coordsForOpenLSiblingUseThreeSegmentForm() {
        assertEquals("com.example:core:1.0.0",
                PomlessMojo.mavenArtifactCoords(dep("com.example", "core", "1.0.0", "zip", null)),
                "OpenL sibling deps use the 3-seg Aether form (default extension zip)");
    }

    @Test
    void coordsForPlainJarIncludeTheTypeSegment() {
        assertEquals("org.apache.commons:commons-text:jar:1.15.0",
                PomlessMojo.mavenArtifactCoords(dep("org.apache.commons", "commons-text", "1.15.0", "jar", null)));
    }

    @Test
    void coordsForJarWithClassifier() {
        assertEquals("com.example:lib:jar:tests:1.0.0",
                PomlessMojo.mavenArtifactCoords(dep("com.example", "lib", "1.0.0", "jar", "tests")));
    }

    @Test
    void coordsForZipWithClassifierKeepsTypeSegmentExplicit() {
        // A 5-seg coordinate must carry the type explicitly — otherwise the classifier would be parsed as the type.
        assertEquals("com.example:rules:zip:variant:1.0.0",
                PomlessMojo.mavenArtifactCoords(dep("com.example", "rules", "1.0.0", "zip", "variant")));
    }

    @Test
    void coordsForNullTypeDefaultsToJar() {
        assertEquals("com.example:lib:jar:1.0.0",
                PomlessMojo.mavenArtifactCoords(dep("com.example", "lib", "1.0.0", null, null)),
                "a dependency with no <type> is treated as a plain jar (Maven's default)");
    }

    // ---- findMatchingNameEntry -----------------------------------------------------------------
    // Sibling-name merge: the migrator looks up the sibling reactor project's logical <name> (from its
    // rules.xml) and feeds that into findMatchingNameEntry — NOT the Maven artifactId. The match is by
    // exact name, and only entries that don't yet declare a <mavenArtifact> are eligible.

    private static ProjectDependencyDescriptor openLDep(String name, String mavenArtifact) {
        var d = new ProjectDependencyDescriptor();
        d.setName(name);
        d.setMavenArtifact(mavenArtifact);
        return d;
    }

    @Test
    void matchesByExactLogicalName() {
        // The CIDP case: the consumer references the sibling by its rules.xml <name> ("DocGen Mapping
        // Common"), not by the artifactId ("cidp-docgen-openl-rules").
        var entries = List.of(
                openLDep("DocGen Mapping Common", null),
                openLDep("Other Domain", null));

        var match = PomlessMojo.findMatchingNameEntry(entries, "DocGen Mapping Common");
        assertSame(entries.get(0), match,
                "the merge target is the entry whose <name> matches the sibling's logical name verbatim");
    }

    @Test
    void skipsEntriesThatAlreadyDeclareAMavenArtifact() {
        // Idempotent — once an entry carries an explicit GAV, the migrator never overwrites it.
        var entries = List.of(openLDep("core", "com.example:core:99.99.99"));

        assertNull(PomlessMojo.findMatchingNameEntry(entries, "core"),
                "an existing <mavenArtifact> makes the slot non-mergeable; caller appends a fresh entry instead");
    }

    @Test
    void noMatchReturnsNullSoTheCallerCanAppendAFreshEntry() {
        var entries = List.of(openLDep("other-name", null));

        assertNull(PomlessMojo.findMatchingNameEntry(entries, "core"),
                "no <name> matches → caller falls back to appending a brand-new <dependency>");
    }

    @Test
    void nullOrBlankSiblingNameNeverMatches() {
        // Defensive: when the sibling reactor lookup fails (e.g. not in the reactor, no rules.xml) the
        // siblingName comes through null. The matcher must NOT collapse that into "match anything".
        var entries = List.of(openLDep("anything", null));

        assertNull(PomlessMojo.findMatchingNameEntry(entries, null));
        assertNull(PomlessMojo.findMatchingNameEntry(entries, ""));
        assertNull(PomlessMojo.findMatchingNameEntry(entries, "   "));
    }

    @Test
    void dependencyManagementEntriesMakeItNonPassThrough() {
        var model = passThroughModel();
        var dm = new DependencyManagement();
        var dep = new Dependency();
        dep.setGroupId("g");
        dep.setArtifactId("a");
        dep.setVersion("1");
        dm.addDependency(dep);
        model.setDependencyManagement(dm);
        assertFalse(PomlessMojo.isPassThrough(model));
    }

    // ---- writeRulesXmlDeps ----------------------------------------------------------------------

    @Test
    void writeRulesXmlDepsAppendsFreshSiblingAndBareJar(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve(ProjectDescriptor.FILE_NAME), "<project><name>consumer</name></project>");
        // An OpenL sibling (zip) absent from the reactor and a packaged jar — neither has a matching <name>
        // entry, so both are appended: the sibling with its artifactId as <name>, the jar name-less.
        var plan = new PomlessConverter.Plan("consumer", "g", dir.resolve("pom.xml"), true,
                List.of(dep("com.example", "domain", "1.0.0", "zip", null),
                        dep("org.apache.commons", "commons-text", "1.15.0", "jar", null)),
                List.of(), List.of(), null);
        var mojo = new PomlessMojo();
        setField(mojo, "reactorProjects", List.<MavenProject>of());

        mojo.writeRulesXmlDeps(plan);

        var xml = Files.readString(dir.resolve(ProjectDescriptor.FILE_NAME));
        assertTrue(xml.contains("<name>domain</name>"),
                "an OpenL sibling absent from the reactor is appended with its artifactId as <name>");
        assertTrue(xml.contains("<mavenArtifact>com.example:domain:1.0.0</mavenArtifact>"));
        assertTrue(xml.contains("<mavenArtifact>org.apache.commons:commons-text:jar:1.15.0</mavenArtifact>"),
                "a packaged jar is appended as a name-less <mavenArtifact>");
    }

    // ---- proposeSubAnchors / findPreservingSubAnchor --------------------------------------------
    // When collapsing pass-throughs would shift a leaf's installed groupId, the migrator proposes promoting
    // the highest pass-through ancestor that preserves the original groupId to a sub-anchor.

    @Test
    void proposesSubAnchorWhenCollapseWouldShiftGroupId() {
        var root = anchorAt("com.example", Path.of("/repo"));
        var lookups = anchorAt("com.example.lookups", Path.of("/repo/lookups"));
        var leaf = leafPlan("com.example.lookups", Path.of("/repo/lookups/colombia/leaf"));
        var reactor = Map.of(Path.of("/repo"), root, Path.of("/repo/lookups"), lookups);

        var proposals = PomlessMojo.proposeSubAnchors(Map.of(leaf, root), reactor,
                Set.of(Path.of("/repo/lookups")), Path.of("/repo"));

        assertEquals(1, proposals.size());
        assertSame(lookups, proposals.keySet().iterator().next(),
                "the pass-through that preserves the leaf's original groupId is proposed as a sub-anchor");
        assertEquals(List.of(leaf), proposals.get(lookups));
    }

    @Test
    void noProposalWhenCollapseAnchorAlreadyPreservesGroupId() {
        // The leaf's original groupId already equals the path-derived form the collapse anchor would install.
        var root = anchorAt("com.example", Path.of("/repo"));
        var lookups = anchorAt("com.example.lookups", Path.of("/repo/lookups"));
        var leaf = leafPlan("com.example.lookups.colombia", Path.of("/repo/lookups/colombia/leaf"));
        var reactor = Map.of(Path.of("/repo"), root, Path.of("/repo/lookups"), lookups);

        var proposals = PomlessMojo.proposeSubAnchors(Map.of(leaf, root), reactor,
                Set.of(Path.of("/repo/lookups")), Path.of("/repo"));

        assertTrue(proposals.isEmpty(), "no conflict — the collapse anchor already installs the original groupId");
    }

    @Test
    void noProposalWhenNoPassThroughPreservesGroupId() {
        var root = anchorAt("com.example", Path.of("/repo"));
        var lookups = anchorAt("com.example.lookups", Path.of("/repo/lookups"));
        var leaf = leafPlan("com.unrelated.group", Path.of("/repo/lookups/colombia/leaf"));
        var reactor = Map.of(Path.of("/repo"), root, Path.of("/repo/lookups"), lookups);

        var proposals = PomlessMojo.proposeSubAnchors(Map.of(leaf, root), reactor,
                Set.of(Path.of("/repo/lookups")), Path.of("/repo"));

        assertTrue(proposals.isEmpty(), "no pass-through ancestor can preserve an unrelated groupId");
    }

    @Test
    void noProposalForLeafWithoutGroupId() {
        var root = anchorAt("com.example", Path.of("/repo"));
        var leaf = leafPlan(null, Path.of("/repo/leaf"));

        var proposals = PomlessMojo.proposeSubAnchors(Map.of(leaf, root),
                Map.of(Path.of("/repo"), root), Set.of(), Path.of("/repo"));

        assertTrue(proposals.isEmpty());
    }

    @Test
    void findPreservingSubAnchorReturnsHighestPreservingAncestor() {
        // Both pass-throughs share the leaf's groupId; the highest (nearest the collapse anchor) wins.
        var root = anchorAt("com.example", Path.of("/repo"));
        var outer = anchorAt("com.acme", Path.of("/repo/outer"));
        var inner = anchorAt("com.acme", Path.of("/repo/outer/inner"));
        var leaf = leafPlan("com.acme", Path.of("/repo/outer/inner/leaf"));
        var reactor = Map.of(
                Path.of("/repo"), root,
                Path.of("/repo/outer"), outer,
                Path.of("/repo/outer/inner"), inner);
        var passThroughs = Set.of(Path.of("/repo/outer"), Path.of("/repo/outer/inner"));

        var found = PomlessMojo.findPreservingSubAnchor(leaf, root, reactor, passThroughs, Path.of("/repo"));

        assertSame(outer, found, "the highest pass-through ancestor below the collapse anchor wins");
    }

    @Test
    void findPreservingSubAnchorReturnsNullWhenNoneArePassThroughs() {
        var root = anchorAt("com.example", Path.of("/repo"));
        var lookups = anchorAt("com.example.lookups", Path.of("/repo/lookups"));
        var leaf = leafPlan("com.example.lookups", Path.of("/repo/lookups/colombia/leaf"));
        var reactor = Map.of(Path.of("/repo"), root, Path.of("/repo/lookups"), lookups);

        // /repo/lookups would preserve, but it isn't flagged as a pass-through → not a candidate.
        var found = PomlessMojo.findPreservingSubAnchor(leaf, root, reactor, Set.of(), Path.of("/repo"));

        assertNull(found);
    }

    // ---- ensurePluginConfigured -----------------------------------------------------------------

    @Test
    void addsOpenLPluginWithExtensionsVersionThresholdAndFlatten() {
        var model = new Model();

        PomlessMojo.ensurePluginConfigured(model, 7, "6.1.0", true);

        var plugin = openLPluginOf(model);
        assertNotNull(plugin, "the openl-maven-plugin must be declared on the anchor");
        assertTrue(plugin.isExtensions());
        assertEquals("6.1.0", plugin.getVersion());
        assertEquals("7", configValue(plugin, PackageMojo.DEPENDENCIES_THRESHOLD_PARAM));
        assertEquals("true", configValue(plugin, PrepareRepositoryPomMojo.FLATTEN_GROUP_ID_PARAM));
    }

    @Test
    void keepsExplicitPluginVersionAndOmitsOptionalConfig() {
        var model = new Model();
        var declared = new Plugin();
        declared.setGroupId(OpenLPackagings.PLUGIN_GROUP_ID);
        declared.setArtifactId(OpenLPackagings.PLUGIN_ARTIFACT_ID);
        declared.setVersion("5.0.0");
        var build = new Build();
        build.addPlugin(declared);
        model.setBuild(build);

        PomlessMojo.ensurePluginConfigured(model, null, "6.1.0", false);

        var plugin = openLPluginOf(model);
        assertEquals("5.0.0", plugin.getVersion(), "an explicit plugin version is never overridden");
        assertNull(plugin.getConfiguration(), "no threshold and no flatten → no <configuration> added");
    }

    // ---- removeModules --------------------------------------------------------------------------

    @Test
    void removesModulesResolvingToConvertedDirs() {
        var model = new Model();
        model.addModule("leaf-a");
        model.addModule("leaf-b");

        var changed = PomlessMojo.removeModules(model, Path.of("/repo"), Set.of(Path.of("/repo/leaf-a")));

        assertTrue(changed);
        assertEquals(List.of("leaf-b"), model.getModules());
    }

    @Test
    void removeModulesReturnsFalseWhenNothingResolves() {
        var model = new Model();
        model.addModule("leaf-a");

        assertFalse(PomlessMojo.removeModules(model, Path.of("/repo"), Set.of(Path.of("/repo/other"))));
        assertEquals(List.of("leaf-a"), model.getModules());
    }

    @Test
    void removeModulesReturnsFalseWhenNoModules() {
        assertFalse(PomlessMojo.removeModules(new Model(), Path.of("/repo"), Set.of(Path.of("/repo/x"))));
    }

    // ---- unionHoistDependencies / renderDependencies --------------------------------------------

    @Test
    void unionDedupesHoistDepsAcrossPlans() {
        var shared = dep("com.eisgroup", "message-bundle", "1.0", "tile", null);
        var unique = dep("com.eisgroup", "service-api", "2.0", "jar", null);

        var union = PomlessMojo.unionHoistDependencies(List.of(planWithHoist(shared), planWithHoist(shared, unique)));

        assertEquals(2, union.size(), "deps shared across plans are de-duplicated by coordinates");
    }

    @Test
    void rendersDependencyXmlOmittingDefaultJarTypeAndNulls() {
        var jar = dep("com.example", "lib", "1.0", "jar", null);
        jar.setScope("provided");

        var xml = PomlessMojo.renderDependencies(List.of(jar));

        assertTrue(xml.contains("<groupId>com.example</groupId>"));
        assertTrue(xml.contains("<version>1.0</version>"));
        assertTrue(xml.contains("<scope>provided</scope>"));
        assertFalse(xml.contains("<type>"), "the default jar type is omitted");
        assertFalse(xml.contains("<classifier>"), "no classifier was set");
    }

    @Test
    void rendersNonJarTypeAndClassifier() {
        var tile = dep("com.eisgroup", "bundle", "1.0", "tile", "shaded");

        var xml = PomlessMojo.renderDependencies(List.of(tile));

        assertTrue(xml.contains("<type>tile</type>"), "a non-jar type is rendered");
        assertTrue(xml.contains("<classifier>shaded</classifier>"));
    }

    private static Plugin openLPluginOf(Model model) {
        if (model.getBuild() == null) {
            return null;
        }
        for (var p : model.getBuild().getPlugins()) {
            if (OpenLPackagings.isOpenLPlugin(p.getGroupId(), p.getArtifactId())) {
                return p;
            }
        }
        return null;
    }

    private static String configValue(Plugin plugin, String name) {
        var dom = (Xpp3Dom) plugin.getConfiguration();
        var child = dom == null ? null : dom.getChild(name);
        return child == null ? null : child.getValue();
    }

    private static PomlessConverter.Plan planWithHoist(Dependency... hoist) {
        return new PomlessConverter.Plan("p", "g", Path.of("/repo/p/pom.xml"), true,
                List.of(), List.of(hoist), List.of(), null);
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        var field = PomlessMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
