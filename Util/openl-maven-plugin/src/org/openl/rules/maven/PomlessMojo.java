package org.openl.rules.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Converts classic (pom-ful) OpenL projects in the reactor to the pom-less form.
 * <p>
 * An aggregator goal: runs once from the anchor it's invoked on, scanning every reactor OpenL project
 * under the anchor's basedir. {@link PomlessConverter} classifies each as <b>convertible</b> (its
 * {@code pom.xml} adds nothing pom-less inheritance can't reproduce) or <b>blocked</b> (per-project
 * plugin config, {@code <executions>}, extra plugins, {@code <profiles>}, custom resources — left
 * untouched and reported).
 * <p>
 * When applied, the goal edits each anchor pom (declares {@code openl-maven-plugin} with
 * {@code <extensions>true</extensions>}, sets {@code <dependenciesThreshold>} to the max across its
 * projects, hoists the convertibles' non-OpenL dependencies), prunes converted modules from every
 * {@code <modules>} list, and deletes the convertible poms.
 * <p>
 * Edited poms are re-serialised, so their comments and formatting are not preserved — review the diff.
 * Dry-run by default; set {@code -Dopenl.pomless.dryRun=false} to apply.
 *
 * @author Yury Molchan
 */
@Mojo(name = "pomless", aggregator = true, threadSafe = true)
public final class PomlessMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    /**
     * When {@code true} (default) nothing is changed — the goal only prints the migration plan. Set to
     * {@code false} to edit the anchor, prune {@code <modules>}, and delete the convertible poms.
     */
    @Parameter(property = "openl.pomless.dryRun", defaultValue = "true")
    private boolean dryRun;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private Prompter prompter;

    @Override
    public void execute() throws MojoExecutionException {
        var anchorDir = project.getBasedir().toPath().toAbsolutePath().normalize();
        var members = collectClassicOpenLProjects(anchorDir);
        if (members.isEmpty()) {
            getLog().info("No classic (pom-ful) OpenL projects found under '" + project.getArtifactId() + "'.");
            return;
        }

        var convertible = new ArrayList<PomlessConverter.Plan>();
        var blocked = new ArrayList<PomlessConverter.Plan>();
        for (var member : members) {
            var plan = PomlessConverter.analyze(member);
            (plan.deletable() ? convertible : blocked).add(plan);
        }

        var plan = planMigration(convertible, blocked);
        report(convertible, blocked, plan);

        if (dryRun) {
            getLog().info("");
            getLog().info("Dry-run: no files changed. Re-run with -Dopenl.pomless.dryRun=false to apply.");
            return;
        }
        if (plan.isEmpty()) {
            getLog().info("Nothing to convert.");
            return;
        }
        apply(plan, openLPluginVersion(members));
    }

    /**
     * The {@code openl-maven-plugin} version the members already resolve to (declared or inherited). Used to
     * pin the plugin when the migrator adds it to an anchor that didn't declare one, so the synthesised
     * pom-less projects bind to that version instead of LATEST. Returns {@code null} when no member exposes one.
     */
    private static String openLPluginVersion(List<MavenProject> members) {
        for (var member : members) {
            for (var plugin : member.getBuildPlugins()) {
                if (OpenLPackagings.isOpenLPlugin(plugin.getGroupId(), plugin.getArtifactId())
                        && plugin.getVersion() != null) {
                    return plugin.getVersion();
                }
            }
        }
        return null;
    }

    private List<MavenProject> collectClassicOpenLProjects(Path anchorDir) {
        var result = new ArrayList<MavenProject>();
        for (var p : reactorProjects) {
            if (p == project) {
                continue;
            }
            if (!OpenLPackagings.isOpenL(p.getPackaging())) {
                continue;
            }
            var file = p.getFile();
            if (file == null || !file.isFile()) {
                continue; // already pom-less (synthesised) — nothing to delete
            }
            var basedir = p.getBasedir();
            if (basedir == null || !basedir.toPath().toAbsolutePath().normalize().startsWith(anchorDir)) {
                continue;
            }
            result.add(p);
        }
        return result;
    }

    // ----------------------------------------------------------------------------------------------
    // Apply
    // ----------------------------------------------------------------------------------------------

    private void apply(MigrationPlan plan, String pluginVersion) throws MojoExecutionException {
        var allConvertibles = new ArrayList<PomlessConverter.Plan>();
        var convertedDirs = new HashSet<Path>();
        for (var edit : plan.anchorEdits()) {
            for (var converted : edit.convertibles()) {
                allConvertibles.add(converted);
                convertedDirs.add(planDir(converted));
            }
        }
        // Surviving anchors prune both converted leaves and collapsed pass-throughs from their <modules>;
        // the pass-through poms are then deleted alongside the leaf poms.
        var deletedDirs = new HashSet<>(convertedDirs);
        deletedDirs.addAll(plan.passThroughDirs());

        for (var converted : allConvertibles) {
            writeRulesXmlDeps(converted);
        }
        var editedAnchorFiles = new HashSet<Path>();
        for (var edit : plan.anchorEdits()) {
            editAnchor(edit, deletedDirs, pluginVersion);
            editedAnchorFiles.add(edit.anchor().getFile().toPath().toAbsolutePath().normalize());
        }
        pruneOtherAggregatorModules(deletedDirs, editedAnchorFiles);
        deletePoms(allConvertibles);
        deletePassThroughPoms(plan.passThroughDirs());
    }

    /** Removes the pass-through aggregator poms after their content has been collapsed onto the anchor. */
    private void deletePassThroughPoms(Set<Path> passThroughDirs) throws MojoExecutionException {
        for (var dir : passThroughDirs) {
            deletePom(dir.resolve("pom.xml"), "(pass-through) ");
        }
    }

    /** Deletes a pom file when present and logs the removal. {@code label} annotates the log line. */
    private void deletePom(Path pom, String label) throws MojoExecutionException {
        try {
            if (Files.deleteIfExists(pom)) {
                getLog().info("Deleted " + label + pom);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to delete '" + pom + "'.", e);
        }
    }

    /**
     * Adds the dependencies the classic pom carried to the project's own {@code rules.xml}.
     * <p>
     * Packaged jars become bare {@code <mavenArtifact>g:a:jar[:classifier]:v</>} entries (no {@code <name>}).
     * OpenL siblings ({@code zip} type) get a 3-seg {@code <mavenArtifact>g:a:v</>}: merged into the existing
     * {@code <name>} entry matching the sibling's logical name when one exists, otherwise appended as a fresh
     * entry carrying both {@code <name>} and {@code <mavenArtifact>}.
     * <p>
     * The file is rewritten via a JAXB round-trip, so comments and whitespace are not preserved.
     * Already-declared coordinates are skipped (idempotent).
     */
    private void writeRulesXmlDeps(PomlessConverter.Plan plan) throws MojoExecutionException {
        if (plan.rulesXmlDeps().isEmpty()) {
            return;
        }
        var rulesXml = plan.pomFile().getParent().resolve(ProjectDescriptor.FILE_NAME);
        var descriptor = ProjectDescriptor.read(rulesXml);
        if (descriptor == null) {
            throw new MojoExecutionException("Cannot read '" + rulesXml + "' to update dependencies.");
        }
        if (descriptor.getDependencies() == null) {
            descriptor.setDependencies(new ArrayList<>());
        }
        var existing = descriptor.getDependencies();
        var declaredCoords = new HashSet<String>();
        for (var d : existing) {
            if (d.getMavenArtifact() != null) {
                declaredCoords.add(d.getMavenArtifact());
            }
        }

        var merged = 0;
        var appended = 0;
        for (var dep : plan.rulesXmlDeps()) {
            var coords = mavenArtifactCoords(dep);
            if (declaredCoords.contains(coords)) {
                continue; // already declared
            }
            var isSibling = OpenLPackagings.ZIP_DEPENDENCY_TYPE.equals(dep.getType());
            if (isSibling) {
                var siblingName = lookupSiblingName(dep);
                var match = findMatchingNameEntry(existing, siblingName);
                if (match != null) {
                    match.setMavenArtifact(coords);
                    merged++;
                } else {
                    var newDep = new ProjectDependencyDescriptor();
                    // Keep the sibling's logical <name> for reactor name-resolution; fall back to artifactId
                    // when the sibling isn't in the reactor.
                    newDep.setName(siblingName != null ? siblingName : dep.getArtifactId());
                    newDep.setMavenArtifact(coords);
                    existing.add(newDep);
                    appended++;
                }
            } else {
                // Bare jar — a name-less <mavenArtifact> is treated as a plain jar on the classpath.
                var newDep = new ProjectDependencyDescriptor();
                newDep.setMavenArtifact(coords);
                existing.add(newDep);
                appended++;
            }
            declaredCoords.add(coords);
        }

        if (merged == 0 && appended == 0) {
            return;
        }
        try {
            Files.write(rulesXml, descriptor.toBytes());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write '" + rulesXml + "'.", e);
        }
        var msg = new StringBuilder("Updated ").append(rulesXml);
        if (merged > 0) {
            msg.append(" — merged ").append(merged).append(" <mavenArtifact> into existing OpenL <name> entry(ies)");
        }
        if (appended > 0) {
            msg.append(merged > 0 ? "," : " —").append(" appended ").append(appended).append(" new entry(ies)");
        }
        getLog().info(msg.toString());
    }

    /**
     * Returns the logical {@code <name>} from the sibling reactor project's {@code rules.xml} — the name the
     * consumer references in {@code <dependency><name>…</name></dependency>}. The lookup is by
     * {@code groupId:artifactId}, needed because the rules.xml {@code <name>} is decoupled from Maven's
     * artifactId. Returns {@code null} when the sibling isn't in the reactor or has no readable {@code <name>};
     * callers then fall back to the artifactId.
     */
    private String lookupSiblingName(Dependency dep) {
        for (var p : reactorProjects) {
            if (p == project) {
                continue;
            }
            if (!OpenLPackagings.isOpenL(p.getPackaging())) {
                continue;
            }
            if (!dep.getGroupId().equals(p.getGroupId()) || !dep.getArtifactId().equals(p.getArtifactId())) {
                continue;
            }
            var basedir = p.getBasedir();
            if (basedir == null) {
                return null;
            }
            var sibling = ProjectDescriptor.read(basedir.toPath());
            if (sibling == null) {
                return null;
            }
            var name = sibling.getName();
            return name == null || name.isBlank() ? null : name;
        }
        return null;
    }

    /**
     * Finds the first dependency whose {@code <name>} equals {@code siblingName} and that has no
     * {@code <mavenArtifact>} yet — the slot to fill. Returns {@code null} when {@code siblingName} is
     * null/blank or no such entry exists; the caller appends a fresh entry instead.
     */
    static ProjectDependencyDescriptor findMatchingNameEntry(List<ProjectDependencyDescriptor> deps,
                                                             String siblingName) {
        if (siblingName == null || siblingName.isBlank()) {
            return null;
        }
        for (var d : deps) {
            if (siblingName.equals(d.getName()) && d.getMavenArtifact() == null) {
                return d;
            }
        }
        return null;
    }

    /**
     * Aether {@code DefaultArtifact} format: {@code g:a:v} for OpenL siblings (3-seg, default extension
     * {@code zip}), {@code g:a:jar[:classifier]:v} for plain jars. Version is always last; classifier (if
     * present on a jar dep) sits between {@code jar} and the version.
     */
    static String mavenArtifactCoords(Dependency dep) {
        var type = dep.getType();
        if (type == null || type.isBlank()) {
            type = OpenLPackagings.JAR_DEPENDENCY_TYPE; // Maven's default
        }
        var classifier = dep.getClassifier();
        var hasClassifier = classifier != null && !classifier.isBlank();
        var sb = new StringBuilder().append(dep.getGroupId()).append(':').append(dep.getArtifactId());
        // Omit the type segment only for the canonical OpenL form (zip + no classifier) — then 3-seg.
        if (!OpenLPackagings.ZIP_DEPENDENCY_TYPE.equals(type) || hasClassifier) {
            sb.append(':').append(type);
        }
        if (hasClassifier) {
            sb.append(':').append(classifier);
        }
        return sb.append(':').append(dep.getVersion()).toString();
    }

    /** Anchor: ensure the plugin (extensions + version + threshold + flattenGroupId), hoist deps, drop modules. */
    private void editAnchor(AnchorEdit edit, Set<Path> deletedDirs, String pluginVersion)
            throws MojoExecutionException {
        var anchor = edit.anchor();
        var model = anchor.getOriginalModel().clone();
        ensurePluginConfigured(model, edit.threshold(), pluginVersion, edit.flattenGroupId());
        hoistDependencies(model, edit.hoist());
        removeModules(model, anchor.getBasedir().toPath().toAbsolutePath().normalize(), deletedDirs);
        writeModel(anchor.getFile().toPath(), model);
    }

    /**
     * Groups the convertible projects by their <b>collapse anchor</b> — the nearest non-pass-through reactor
     * ancestor (or {@code ${project}} when all ancestors collapse). A pass-through aggregator that still hosts
     * a surviving descendant stays a real anchor so its descendants' {@code <parent>} chain isn't stranded.
     * Each anchor's {@code <dependenciesThreshold>} is reconciled to the max across the OpenL projects under
     * it, and {@code <flattenGroupId>} is set per the {@link #inferFlattenGroupId} heuristic.
     */
    private MigrationPlan planMigration(List<PomlessConverter.Plan> convertible,
                                        List<PomlessConverter.Plan> blocked) throws MojoExecutionException {
        var anchorDir = project.getBasedir().toPath().toAbsolutePath().normalize();
        var reactorByDir = indexReactorByDir();
        // Keep any pass-through that still has a surviving descendant so the survivor's <parent> stays valid.
        var passThroughDirs = identifyPassThroughs(anchorDir);
        var survivorDirs = identifySurvivors(anchorDir, passThroughDirs, blocked);
        retainOnlyDeletable(passThroughDirs, survivorDirs);

        // First pass: assign each leaf to its collapse anchor — the fallback when a sub-anchor is declined.
        var leafToCollapseAnchor = new LinkedHashMap<PomlessConverter.Plan, MavenProject>();
        for (var plan : convertible) {
            var anchor = collapseAnchorOf(planDir(plan), reactorByDir, passThroughDirs, anchorDir, new HashSet<>());
            if (anchor == null) {
                getLog().warn("Skipping '" + plan.artifactId()
                        + "': found no ancestor pom to host the openl-maven-plugin anchor.");
                continue;
            }
            leafToCollapseAnchor.put(plan, anchor);
        }
        if (leafToCollapseAnchor.isEmpty()) {
            return new MigrationPlan(List.of(), Set.of());
        }

        // For each leaf whose collapse anchor would shift its groupId, propose the highest pass-through
        // ancestor that would preserve it as a sub-anchor; the user confirms each (Y default).
        var subAnchorProposals = proposeSubAnchors(leafToCollapseAnchor, reactorByDir, passThroughDirs, anchorDir);
        var confirmedSubAnchors = promptForSubAnchors(subAnchorProposals);

        // Promoted sub-anchors are removed from the deletion set; they become real anchors.
        for (var subAnchor : confirmedSubAnchors) {
            passThroughDirs.remove(subAnchor.getBasedir().toPath().toAbsolutePath().normalize());
        }

        // Final assignment: with confirmed sub-anchors removed from passThroughDirs, collapseAnchorOf now
        // stops at them naturally; every other leaf stays at its collapse anchor.
        var convByAnchor = new LinkedHashMap<MavenProject, List<PomlessConverter.Plan>>();
        for (var plan : convertible) {
            if (!leafToCollapseAnchor.containsKey(plan)) {
                continue; // skipped earlier (no ancestor pom)
            }
            var anchor = collapseAnchorOf(planDir(plan), reactorByDir, passThroughDirs, anchorDir, new HashSet<>());
            if (anchor == null) {
                continue;
            }
            convByAnchor.computeIfAbsent(anchor, a -> new ArrayList<>()).add(plan);
        }
        // Reconcile <dependenciesThreshold> per anchor across all OpenL projects under it (convertible +
        // blocked) — the max value never fails a stricter project that stays classic.
        var thresholds = new HashMap<MavenProject, Integer>();
        for (var plan : convertible) {
            recordThreshold(plan, reactorByDir, passThroughDirs, anchorDir, convByAnchor.keySet(), thresholds);
        }
        for (var plan : blocked) {
            recordThreshold(plan, reactorByDir, passThroughDirs, anchorDir, convByAnchor.keySet(), thresholds);
        }
        var edits = new ArrayList<AnchorEdit>(convByAnchor.size());
        for (var entry : convByAnchor.entrySet()) {
            var anchor = entry.getKey();
            var anchorLeaves = entry.getValue();
            edits.add(new AnchorEdit(anchor, thresholds.get(anchor),
                    unionHoistDependencies(anchorLeaves), anchorLeaves,
                    inferFlattenGroupId(anchor, anchorLeaves)));
        }
        return new MigrationPlan(edits, passThroughDirs);
    }

    /**
     * For each leaf whose collapse anchor would change its installed groupId, returns the highest pass-through
     * ancestor that would preserve the original groupId — the sub-anchor the user is asked to confirm. Leaves
     * the heuristic already preserves, and pass-throughs that wouldn't help, are not proposed.
     */
    private Map<MavenProject, List<PomlessConverter.Plan>> proposeSubAnchors(
            Map<PomlessConverter.Plan, MavenProject> leafToCollapseAnchor,
            Map<Path, MavenProject> reactorByDir,
            Set<Path> passThroughDirs,
            Path anchorDir) {
        // Group leaves by collapse anchor and pre-compute the heuristic flatten the anchor would settle on.
        var byAnchor = new LinkedHashMap<MavenProject, List<PomlessConverter.Plan>>();
        for (var e : leafToCollapseAnchor.entrySet()) {
            byAnchor.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        var flattenByAnchor = new HashMap<MavenProject, Boolean>();
        for (var e : byAnchor.entrySet()) {
            flattenByAnchor.put(e.getKey(), inferFlattenGroupId(e.getKey(), e.getValue()));
        }

        var proposals = new LinkedHashMap<MavenProject, List<PomlessConverter.Plan>>();
        for (var entry : leafToCollapseAnchor.entrySet()) {
            var leaf = entry.getKey();
            var collapseAnchor = entry.getValue();
            if (leaf.groupId() == null) {
                continue;
            }
            var flatten = flattenByAnchor.get(collapseAnchor);
            if (leaf.groupId().equals(installedGroupId(collapseAnchor, flatten, planDir(leaf)))) {
                continue; // already preserved by the heuristic's chosen flatten — no conflict
            }
            var subAnchor = findPreservingSubAnchor(leaf, collapseAnchor, reactorByDir, passThroughDirs, anchorDir);
            if (subAnchor != null) {
                proposals.computeIfAbsent(subAnchor, k -> new ArrayList<>()).add(leaf);
            }
        }
        return proposals;
    }

    /** The installed groupId the leaf would get if anchored at {@code anchor} with the given flatten choice. */
    private static String installedGroupId(MavenProject anchor, boolean flatten, Path leafFolder) {
        var anchorGroup = anchor.getGroupId();
        if (flatten || anchor.getBasedir() == null) {
            return anchorGroup;
        }
        return pathDerivedGroupId(anchorGroup, anchor.getBasedir().toPath().toAbsolutePath().normalize(), leafFolder);
    }

    /**
     * Returns the highest pass-through ancestor below {@code leaf}'s collapse anchor whose pom would preserve
     * the leaf's original groupId, or {@code null} when none exists.
     */
    private static MavenProject findPreservingSubAnchor(PomlessConverter.Plan leaf,
                                                        MavenProject collapseAnchor,
                                                        Map<Path, MavenProject> reactorByDir,
                                                        Set<Path> passThroughDirs,
                                                        Path anchorDir) {
        var collapseDir = collapseAnchor.getBasedir().toPath().toAbsolutePath().normalize();
        MavenProject highest = null;
        for (var dir = planDir(leaf).getParent(); dir != null && !dir.equals(collapseDir.getParent()); dir = dir.getParent()) {
            if (dir.equals(collapseDir)) {
                break; // sub-anchor must be strictly below the collapse anchor
            }
            if (!passThroughDirs.contains(dir)) {
                continue; // only pass-throughs are candidates to be promoted
            }
            if (!dir.startsWith(anchorDir)) {
                break;
            }
            var candidate = reactorByDir.get(dir);
            if (candidate == null || OpenLPackagings.isOpenL(candidate.getPackaging())) {
                continue;
            }
            if (preserves(candidate, planDir(leaf), leaf.groupId())) {
                highest = candidate;
            }
        }
        return highest;
    }

    /** True iff anchoring {@code leaf} at {@code anchor} (with some flatten choice) preserves its groupId. */
    private static boolean preserves(MavenProject anchor, Path leafFolder, String origGroupId) {
        var anchorGroup = anchor.getGroupId();
        if (anchorGroup == null || origGroupId == null) {
            return false;
        }
        if (anchorGroup.equals(origGroupId)) {
            return true; // flatten=true at this level matches
        }
        var anchorBasedir = anchor.getBasedir();
        if (anchorBasedir == null) {
            return false;
        }
        var path = pathDerivedGroupId(anchorGroup, anchorBasedir.toPath().toAbsolutePath().normalize(), leafFolder);
        return path.equals(origGroupId);
    }

    /**
     * Asks the user (Y/n, default Y) whether to promote each proposed sub-anchor. Batch mode and a missing
     * prompter both default to Y. A declined sub-anchor's leaves stay at the collapse anchor, so their
     * installed groupId shifts to the anchor's flatten choice.
     */
    private Set<MavenProject> promptForSubAnchors(Map<MavenProject, List<PomlessConverter.Plan>> proposals)
            throws MojoExecutionException {
        var confirmed = new HashSet<MavenProject>();
        if (proposals.isEmpty()) {
            return confirmed;
        }
        var interactive = session.getRequest() != null && session.getRequest().isInteractiveMode();
        for (var entry : proposals.entrySet()) {
            var subAnchor = entry.getKey();
            var leaves = entry.getValue();
            var names = leaves.stream().map(PomlessConverter.Plan::artifactId).toList();
            getLog().info("");
            getLog().info("Sub-anchor proposal at '" + subAnchor.getBasedir() + "':");
            getLog().info("  Preserves the original groupId for: " + String.join(", ", names));
            getLog().info("  Without it, those leaves anchor higher up and their installed groupIds shift");
            getLog().info("  to whatever the parent anchor's majority/most-compatible flatten yields.");
            if (!interactive) {
                getLog().info("  [batch mode → default Y: sub-anchor preserved]");
                confirmed.add(subAnchor);
                continue;
            }
            if (askYesNo("Create an anchor pom at '" + subAnchor.getArtifactId() + "'? [Y/n]")) {
                confirmed.add(subAnchor);
            }
        }
        return confirmed;
    }

    /** Interactive Y/n prompt (Y default). Falls back to Y when the prompter is unavailable. */
    private boolean askYesNo(String message) throws MojoExecutionException {
        if (prompter == null) {
            getLog().warn("Prompter unavailable; defaulting to Y for: " + message);
            return true;
        }
        try {
            var answer = prompter.prompt(message, "Y");
            return answer == null || answer.trim().isEmpty() || answer.trim().equalsIgnoreCase("Y");
        } catch (PrompterException e) {
            throw new MojoExecutionException("Failed to read user response for: " + message, e);
        }
    }

    /**
     * Reactor projects that keep their {@code pom.xml} after migration: {@code ${project}}, every
     * non-pass-through aggregator under it, and every blocked OpenL leaf. Pass-through aggregators and
     * converted leaves are not survivors — their poms are deleted.
     */
    private Set<Path> identifySurvivors(Path anchorDir, Set<Path> passThroughDirs,
                                        List<PomlessConverter.Plan> blocked) {
        var survivors = new HashSet<Path>();
        survivors.add(anchorDir);
        for (var p : reactorProjects) {
            if (p == project || p.getFile() == null || p.getBasedir() == null) {
                continue;
            }
            if (OpenLPackagings.isOpenL(p.getPackaging())) {
                continue; // OpenL leaves are handled by the blocked list below
            }
            var dir = p.getBasedir().toPath().toAbsolutePath().normalize();
            if (!dir.startsWith(anchorDir) || passThroughDirs.contains(dir)) {
                continue;
            }
            survivors.add(dir);
        }
        for (var plan : blocked) {
            survivors.add(planDir(plan));
        }
        return survivors;
    }

    /**
     * Drops from {@code passThroughDirs} any aggregator with a survivor strictly under it — deleting it would
     * orphan the survivor's {@code <parent>} chain. Such a pass-through stays and becomes an effective anchor.
     */
    private static void retainOnlyDeletable(Set<Path> passThroughDirs, Set<Path> survivorDirs) {
        passThroughDirs.removeIf(d -> {
            for (var s : survivorDirs) {
                if (!s.equals(d) && s.startsWith(d)) {
                    return true; // must keep — a survivor lives under this aggregator
                }
            }
            return false;
        });
    }

    /**
     * Compatibility heuristic for {@code <flattenGroupId>}: returns {@code true} when more of the anchor's
     * leaves' original groupIds match the anchor groupId verbatim than match the default path-derived form
     * ({@code anchorGroup + ".<dotted-path>"}). This keeps installed coordinates stable across the collapse —
     * flat when leaves share one corporate groupId, path-derived when they encode the directory path. Returns
     * {@code false} on a tie (the default needs no extra config).
     */
    static boolean inferFlattenGroupId(MavenProject anchor, List<PomlessConverter.Plan> leaves) {
        var anchorGroup = anchor.getGroupId();
        if (anchorGroup == null || leaves.isEmpty()) {
            return false;
        }
        var anchorDir = anchor.getBasedir().toPath().toAbsolutePath().normalize();
        int matchesFlat = 0;
        int matchesPath = 0;
        for (var leaf : leaves) {
            var orig = leaf.groupId();
            if (orig == null) {
                continue;
            }
            if (anchorGroup.equals(orig)) {
                matchesFlat++;
            }
            if (pathDerivedGroupId(anchorGroup, anchorDir, planDir(leaf)).equals(orig)) {
                matchesPath++;
            }
        }
        return matchesFlat > matchesPath;
    }

    /**
     * Dotted intermediate path from {@code anchorDir} down to the leaf's containing folder — the same form
     * {@code OpenLCoordinates.of} appends to the anchor's groupId when {@code flattenGroupId=false}. Empty when
     * the leaf folder sits directly under the anchor.
     */
    private static String dottedPath(Path anchorDir, Path leafFolder) {
        var parent = leafFolder.getParent();
        if (parent == null) {
            return "";
        }
        var rel = anchorDir.relativize(parent).toString();
        if (rel.isEmpty()) {
            return "";
        }
        return rel.replace(java.io.File.separatorChar, '.').replace('/', '.');
    }

    /**
     * The path-derived groupId {@code OpenLCoordinates.of} would produce for a leaf at {@code leafFolder}
     * anchored at {@code anchorDir} with {@code flattenGroupId=false}: {@code anchorGroup} plus the dotted
     * intermediate path, or {@code anchorGroup} verbatim when the leaf sits directly under the anchor.
     */
    private static String pathDerivedGroupId(String anchorGroup, Path anchorDir, Path leafFolder) {
        var dotted = dottedPath(anchorDir, leafFolder);
        return dotted.isEmpty() ? anchorGroup : anchorGroup + '.' + dotted;
    }

    private static void recordThreshold(PomlessConverter.Plan plan, Map<Path, MavenProject> reactorByDir,
                                        Set<Path> passThroughDirs, Path invocationRootDir,
                                        Set<MavenProject> anchors, Map<MavenProject, Integer> thresholds) {
        if (plan.dependenciesThreshold() == null) {
            return;
        }
        var anchor = collapseAnchorOf(planDir(plan), reactorByDir, passThroughDirs, invocationRootDir,
                new HashSet<>());
        if (anchor == null || !anchors.contains(anchor)) {
            return; // a threshold only matters for an anchor we actually edit
        }
        thresholds.merge(anchor, plan.dependenciesThreshold(), Math::max);
    }

    /** The full migration plan — the per-anchor edits plus the pass-through aggregators to delete. */
    private record MigrationPlan(List<AnchorEdit> anchorEdits, Set<Path> passThroughDirs) {
        boolean isEmpty() {
            return anchorEdits.isEmpty();
        }
    }

    /** Indexes reactor projects backed by a real pom, keyed by their normalised basedir. */
    private Map<Path, MavenProject> indexReactorByDir() {
        var map = new HashMap<Path, MavenProject>();
        for (var p : reactorProjects) {
            if (p.getFile() == null || p.getBasedir() == null) {
                continue; // already pom-less (synthesised) — no pom to anchor on
            }
            map.putIfAbsent(p.getBasedir().toPath().toAbsolutePath().normalize(), p);
        }
        return map;
    }

    /**
     * Returns the first non-pass-through ancestor reactor project above {@code projectDir}, walking up through
     * pass-through aggregators. {@code invocationRootDir} ({@code ${project}}) is always a valid anchor — we
     * cannot reach above it. Every pass-through walked past is added to {@code collapsedOut} for the caller to
     * delete. Returns {@code null} when no ancestor reactor pom exists.
     */
    static MavenProject collapseAnchorOf(Path projectDir,
                                         Map<Path, MavenProject> reactorByDir,
                                         Set<Path> passThroughDirs,
                                         Path invocationRootDir,
                                         Set<Path> collapsedOut) {
        for (var dir = projectDir.getParent(); dir != null; dir = dir.getParent()) {
            var candidate = reactorByDir.get(dir);
            if (candidate == null || OpenLPackagings.isOpenL(candidate.getPackaging())) {
                continue;
            }
            if (dir.equals(invocationRootDir)) {
                return candidate; // ${project} is the final stop — always anchorable
            }
            if (passThroughDirs.contains(dir)) {
                collapsedOut.add(dir);
                continue;
            }
            return candidate;
        }
        return null;
    }

    /**
     * True when {@code model} is a {@code pom}-packaging aggregator whose body is purely structural
     * (parent/GAV/modules/metadata) — no build, plugins, profiles, properties, dependencies,
     * dependencyManagement, repositories, reporting, or distributionManagement. Such poms only organise the
     * directory tree and can be deleted when collapsing the migration onto a single anchor.
     */
    static boolean isPassThrough(Model model) {
        if (model == null || !"pom".equals(model.getPackaging())) {
            return false;
        }
        var build = model.getBuild();
        if (build != null) {
            if (build.getPlugins() != null && !build.getPlugins().isEmpty()) return false;
            var pm = build.getPluginManagement();
            if (pm != null && pm.getPlugins() != null && !pm.getPlugins().isEmpty()) return false;
            if (build.getFinalName() != null) return false;
            if (build.getSourceDirectory() != null) return false;
            if (build.getScriptSourceDirectory() != null) return false;
            if (build.getTestSourceDirectory() != null) return false;
            if (build.getResources() != null && !build.getResources().isEmpty()) return false;
            if (build.getTestResources() != null && !build.getTestResources().isEmpty()) return false;
            if (build.getExtensions() != null && !build.getExtensions().isEmpty()) return false;
            if (build.getFilters() != null && !build.getFilters().isEmpty()) return false;
        }
        if (model.getProperties() != null && !model.getProperties().isEmpty()) return false;
        if (model.getProfiles() != null && !model.getProfiles().isEmpty()) return false;
        if (model.getDependencies() != null && !model.getDependencies().isEmpty()) return false;
        var dm = model.getDependencyManagement();
        if (dm != null && dm.getDependencies() != null && !dm.getDependencies().isEmpty()) return false;
        if (model.getRepositories() != null && !model.getRepositories().isEmpty()) return false;
        if (model.getPluginRepositories() != null && !model.getPluginRepositories().isEmpty()) return false;
        if (model.getReporting() != null) return false;
        return model.getDistributionManagement() == null;
    }

    /**
     * The set of pass-through aggregator basedirs strictly under {@code anchorDir}. {@code ${project}} itself
     * is excluded — it is never deleted; it becomes the surviving anchor when its descendants collapse to it.
     */
    private Set<Path> identifyPassThroughs(Path anchorDir) {
        var result = new HashSet<Path>();
        for (var p : reactorProjects) {
            if (p == project) {
                continue;
            }
            if (p.getFile() == null || p.getBasedir() == null) {
                continue;
            }
            if (OpenLPackagings.isOpenL(p.getPackaging())) {
                continue;
            }
            var dir = p.getBasedir().toPath().toAbsolutePath().normalize();
            if (!dir.startsWith(anchorDir)) {
                continue;
            }
            if (isPassThrough(p.getOriginalModel())) {
                result.add(dir);
            }
        }
        return result;
    }

    private static Path planDir(PomlessConverter.Plan plan) {
        return plan.pomFile().getParent().toAbsolutePath().normalize();
    }

    /** Reactor aggregators other than the edited anchors may also list converted projects in {@code <modules>}. */
    private void pruneOtherAggregatorModules(Set<Path> convertedDirs, Set<Path> editedAnchorFiles)
            throws MojoExecutionException {
        for (var p : reactorProjects) {
            if (p.getFile() == null) {
                continue;
            }
            var file = p.getFile().toPath().toAbsolutePath().normalize();
            if (editedAnchorFiles.contains(file)) {
                continue; // already pruned while editing it as an anchor
            }
            var original = p.getOriginalModel();
            if (original.getModules() == null || original.getModules().isEmpty()) {
                continue;
            }
            var model = original.clone();
            if (removeModules(model, p.getBasedir().toPath().toAbsolutePath().normalize(), convertedDirs)) {
                writeModel(file, model);
            }
        }
    }

    /**
     * A single anchor pom to edit: where the plugin, threshold, and hoisted deps land, plus its convertible
     * members and the resolved {@code flattenGroupId} flag.
     */
    private record AnchorEdit(MavenProject anchor, Integer threshold, List<Dependency> hoist,
                              List<PomlessConverter.Plan> convertibles, boolean flattenGroupId) {
    }

    private static void ensurePluginConfigured(Model model, Integer maxThreshold, String pluginVersion,
                                               boolean flattenGroupId) {
        var build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }
        Plugin plugin = null;
        for (var p : build.getPlugins()) {
            if (OpenLPackagings.isOpenLPlugin(p.getGroupId(), p.getArtifactId())) {
                plugin = p;
                break;
            }
        }
        if (plugin == null) {
            plugin = new Plugin();
            plugin.setGroupId(OpenLPackagings.PLUGIN_GROUP_ID);
            plugin.setArtifactId(OpenLPackagings.PLUGIN_ARTIFACT_ID);
            build.addPlugin(plugin);
        }
        plugin.setExtensions(true);
        // Pin a version only when the anchor didn't declare one — the participant binds the pom-less projects
        // to it. Never overrides an explicit version.
        if (plugin.getVersion() == null && pluginVersion != null) {
            plugin.setVersion(pluginVersion);
        }
        if (maxThreshold != null) {
            setConfigChild(plugin, PackageMojo.DEPENDENCIES_THRESHOLD_PARAM, Integer.toString(maxThreshold));
        }
        if (flattenGroupId) {
            // Use the anchor's groupId verbatim for every pom-less leaf below it (skip the dotted path
            // derivation) so installed coordinates don't shift because of the collapse.
            setConfigChild(plugin, PrepareRepositoryPomMojo.FLATTEN_GROUP_ID_PARAM, "true");
        }
    }

    private static void setConfigChild(Plugin plugin, String name, String value) {
        var config = plugin.getConfiguration() instanceof Xpp3Dom dom ? dom : new Xpp3Dom("configuration");
        var child = config.getChild(name);
        if (child == null) {
            child = new Xpp3Dom(name);
            config.addChild(child);
        }
        child.setValue(value);
        plugin.setConfiguration(config);
    }

    private static void hoistDependencies(Model model, List<Dependency> hoist) {
        if (hoist.isEmpty()) {
            return;
        }
        var existing = new HashSet<String>();
        for (var d : model.getDependencies()) {
            existing.add(dependencyKey(d));
        }
        for (var d : hoist) {
            if (existing.add(dependencyKey(d))) {
                model.addDependency(d);
            }
        }
    }

    /** Removes {@code <module>} entries that resolve (against {@code baseDir}) to a converted project. */
    private static boolean removeModules(Model model, Path baseDir, Set<Path> convertedDirs) {
        var modules = model.getModules();
        if (modules == null || modules.isEmpty()) {
            return false;
        }
        var kept = new ArrayList<String>(modules.size());
        for (var module : modules) {
            var resolved = baseDir.resolve(module).normalize();
            if (!convertedDirs.contains(resolved)) {
                kept.add(module);
            }
        }
        if (kept.size() == modules.size()) {
            return false;
        }
        model.setModules(kept);
        return true;
    }

    private void writeModel(Path pomFile, Model model) throws MojoExecutionException {
        try (var out = Files.newBufferedWriter(pomFile)) {
            new MavenXpp3Writer().write(out, model);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write '" + pomFile + "'.", e);
        }
        getLog().info("Edited " + pomFile);
    }

    private void deletePoms(List<PomlessConverter.Plan> convertible) throws MojoExecutionException {
        for (var plan : convertible) {
            var pom = plan.pomFile();
            if (pom != null) {
                deletePom(pom, "");
            }
        }
        getLog().info("Converted " + convertible.size() + " project(s) to pom-less. Review the edited poms' diff.");
    }

    // ----------------------------------------------------------------------------------------------
    // Reporting
    // ----------------------------------------------------------------------------------------------

    private void report(List<PomlessConverter.Plan> convertible, List<PomlessConverter.Plan> blocked,
                        MigrationPlan migrationPlan) {
        getLog().info("Pom-less migration plan for anchor '" + project.getArtifactId() + "':");
        getLog().info("  convertible: " + convertible.size() + ", blocked: " + blocked.size()
                + ", pass-through aggregators collapsed: " + migrationPlan.passThroughDirs().size());

        if (!convertible.isEmpty()) {
            getLog().info("");
            getLog().info("Convertible — pom.xml will be deleted:");
            for (var plan : convertible) {
                var jars = plan.rulesXmlDeps().isEmpty() ? ""
                        : " (+" + plan.rulesXmlDeps().size() + " jar dep(s) → its rules.xml)";
                getLog().info("  - " + plan.artifactId() + jars);
            }
        }
        for (var edit : migrationPlan.anchorEdits()) {
            getLog().info("");
            var note = edit.flattenGroupId() ? " (with <flattenGroupId>true</> — pass-throughs collapsed)" : "";
            getLog().info("Anchor '" + edit.anchor().getArtifactId() + "' (" + edit.anchor().getFile()
                    + ") — declares openl-maven-plugin <extensions>true</>" + note + ":");
            if (edit.threshold() != null) {
                getLog().info("    <dependenciesThreshold>" + edit.threshold() + "</dependenciesThreshold>");
            }
            if (!edit.hoist().isEmpty()) {
                getLog().info("  Non-OpenL dependencies hoisted to its <dependencies>:");
                getLog().info(renderDependencies(edit.hoist()));
            }
        }
        if (!migrationPlan.passThroughDirs().isEmpty()) {
            getLog().info("");
            getLog().info("Pass-through aggregators — pom.xml will be deleted (empty scaffolding):");
            for (var dir : migrationPlan.passThroughDirs()) {
                getLog().info("  - " + dir.resolve("pom.xml"));
            }
        }
        if (!blocked.isEmpty()) {
            getLog().info("");
            getLog().info("Blocked — left untouched (resolve manually, then re-run):");
            for (var plan : blocked) {
                getLog().info("  - " + plan.artifactId() + ": " + String.join("; ", plan.blockers()));
            }
        }
    }

    /** Union of every convertible project's hoistable deps, de-duplicated by full coordinates. */
    private static List<Dependency> unionHoistDependencies(List<PomlessConverter.Plan> convertible) {
        var byKey = new LinkedHashMap<String, Dependency>();
        for (var plan : convertible) {
            for (var dep : plan.hoistDeps()) {
                byKey.putIfAbsent(dependencyKey(dep), dep);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static String dependencyKey(Dependency dep) {
        return dep.getGroupId() + ':' + dep.getArtifactId() + ':' + dep.getType()
                + ':' + (dep.getClassifier() == null ? "" : dep.getClassifier())
                + ':' + (dep.getScope() == null ? "" : dep.getScope());
    }

    private static String renderDependencies(List<Dependency> deps) {
        var sb = new StringBuilder();
        for (var dep : deps) {
            sb.append("    <dependency>\n");
            sb.append("        <groupId>").append(dep.getGroupId()).append("</groupId>\n");
            sb.append("        <artifactId>").append(dep.getArtifactId()).append("</artifactId>\n");
            if (dep.getVersion() != null) {
                sb.append("        <version>").append(dep.getVersion()).append("</version>\n");
            }
            if (dep.getClassifier() != null) {
                sb.append("        <classifier>").append(dep.getClassifier()).append("</classifier>\n");
            }
            if (dep.getType() != null && !OpenLPackagings.JAR_DEPENDENCY_TYPE.equals(dep.getType())) {
                sb.append("        <type>").append(dep.getType()).append("</type>\n");
            }
            if (dep.getScope() != null) {
                sb.append("        <scope>").append(dep.getScope()).append("</scope>\n");
            }
            if (dep.getOptional() != null) {
                sb.append("        <optional>").append(dep.getOptional()).append("</optional>\n");
            }
            sb.append("    </dependency>\n");
        }
        return sb.toString();
    }
}
