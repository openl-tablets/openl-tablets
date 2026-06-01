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
 * An <b>aggregator</b> goal — runs once from the project it's invoked on (the anchor), scanning every
 * reactor OpenL project ({@code openl}/{@code openl-jar} packaging) under the anchor's basedir. Each is
 * classified by {@link PomlessConverter}:
 * <ul>
 *     <li><b>Convertible</b> — the raw {@code pom.xml} adds nothing the pom-less participant + anchor
 *         inheritance can't reproduce.</li>
 *     <li><b>Blocked</b> — per-project plugin configuration (other than {@code dependenciesThreshold} /
 *         deprecated {@code deploymentPackage}), {@code <executions>}, extra plugins, {@code <profiles>},
 *         custom resources, etc. Left untouched and reported.</li>
 * </ul>
 * When applied (not a dry run) the goal:
 * <ol>
 *     <li>edits the <b>anchor</b> pom — ensures {@code openl-maven-plugin} is declared in
 *         {@code <build><plugins>} with {@code <extensions>true</extensions>}, sets its
 *         {@code <dependenciesThreshold>} to the max across all projects, and adds the union of the
 *         convertible projects' non-OpenL dependencies to {@code <dependencies>};</li>
 *     <li>removes the converted modules from every aggregator {@code <modules>} list;</li>
 *     <li>deletes the convertible projects' {@code pom.xml}.</li>
 * </ol>
 * Pom edits go through {@code MavenXpp3Writer}, which re-serialises the model — comments and exact
 * formatting in the edited poms are not preserved, so review the diff before committing. Dry-run by
 * default; nothing changes until {@code -Dopenl.pomless.dryRun=false}.
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

        // Build the full migration plan: each convertible's collapse-anchor (the nearest non-pass-through
        // ancestor, or ${project}) + the pass-through aggregator poms to delete in the same pass. The
        // openl-maven-plugin declaration, threshold, hoisted deps, and <flattenGroupId> all land on the
        // surviving anchor pom.
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
     * The {@code openl-maven-plugin} version the converted projects already resolve to (from their effective
     * model — declared or inherited). Used to pin the version when the migrator has to add the plugin to an
     * anchor that didn't declare it, so the synthesised pom-less projects bind to the same plugin version
     * rather than letting Maven resolve LATEST. Returns {@code null} when none of the members expose one.
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
        // Surviving anchors filter both the converted OpenL leaves AND the collapsed pass-through aggregators
        // out of their <modules>; pass-through poms themselves are then deleted alongside the leaf poms.
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
     * Updates the project's {@code rules.xml} with the deps the classic pom carried. Two flavours:
     * <ul>
     *   <li><b>Packaged jars</b> ({@code <type>} default or {@code jar}) — appended as bare
     *       {@code <dependency><mavenArtifact>g:a:jar[:classifier]:v</mavenArtifact></dependency>} entries
     *       (no {@code <name>}/{@code <autoIncluded>}; the runtime treats a name-less {@code <mavenArtifact>}
     *       as a plain jar on the classpath, and the synthesiser marks it {@code <optional>true</>}).</li>
     *   <li><b>OpenL siblings</b> ({@code <type>zip</type>}) — looked up by GAV in the reactor; the
     *       sibling's <i>logical</i> {@code <name>} (from its own {@code rules.xml}, e.g. "DocGen Mapping
     *       Common") is then matched against the consumer's existing {@code <dependency><name>…</></>}
     *       entries. On a match, {@code <mavenArtifact>g:a:v</>} (3-seg Aether form, default extension zip)
     *       is set on that entry so the GAV is explicit alongside the OpenL {@code <name>}. Otherwise a
     *       fresh {@code <dependency>} with both {@code <name>} and {@code <mavenArtifact>} is appended.</li>
     * </ul>
     * The file is read and rewritten via {@link ProjectDescriptor#read}/{@link ProjectDescriptor#toBytes}
     * (JAXB round-trip) — comments and whitespace are not preserved, but the structure and field order are
     * stable. Already-declared coordinates are skipped (idempotent).
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
                    // Use the sibling's logical <name> when available (so reactor name-resolution still works
                    // for projects that re-deploy without an explicit mavenArtifact). Fall back to artifactId
                    // when the sibling isn't in the reactor — we couldn't read its rules.xml to learn the name.
                    newDep.setName(siblingName != null ? siblingName : dep.getArtifactId());
                    newDep.setMavenArtifact(coords);
                    existing.add(newDep);
                    appended++;
                }
            } else {
                // Bare jar — no <name>/<autoIncluded>; the runtime treats a name-less <mavenArtifact> as a
                // plain jar on the classpath.
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
     * Reads the sibling reactor project's {@code rules.xml} (located by matching {@code groupId:artifactId}
     * against {@link MavenProject#getGroupId()}/{@link MavenProject#getArtifactId()}) and returns its
     * logical {@code <name>} — which the consumer's {@code rules.xml} references in
     * {@code <dependency><name>…</name></dependency>}. The reactor lookup is necessary because the rules.xml
     * {@code <name>} is decoupled from Maven's artifactId (e.g. artifactId {@code cidp-docgen-openl-rules}
     * with {@code <name>DocGen Mapping Common</>}). Returns {@code null} when the sibling isn't in the
     * reactor, has no basedir, has no readable {@code rules.xml}, or its rules.xml carries no {@code <name>}
     * — callers then fall back to the artifactId.
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
     * Finds the first {@link ProjectDependencyDescriptor} whose {@code <name>} equals {@code siblingName}
     * and that doesn't yet declare an explicit {@code <mavenArtifact>} — the slot we want to fill. Returns
     * {@code null} when {@code siblingName} is null/blank or no matching entry exists; the caller then
     * appends a fresh entry instead.
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
     * ancestor (or {@code ${project}} if all ancestors collapse). Pass-through aggregators that <i>still host a
     * surviving descendant</i> (a blocked OpenL leaf that stays classic, or a non-pass-through sub-aggregator)
     * are <i>not</i> collapsible — they remain real anchors so their descendants' {@code <parent>} chain isn't
     * stranded. Each anchor's {@code <dependenciesThreshold>} is reconciled to the maximum across the OpenL
     * projects under it. The {@code <flattenGroupId>true</>} flag is set per anchor via a compatibility
     * heuristic — only when it preserves the leaves' original groupIds better than the default path-derived
     * derivation would (see {@link #inferFlattenGroupId}).
     */
    private MigrationPlan planMigration(List<PomlessConverter.Plan> convertible,
                                        List<PomlessConverter.Plan> blocked) throws MojoExecutionException {
        var anchorDir = project.getBasedir().toPath().toAbsolutePath().normalize();
        var reactorByDir = indexReactorByDir();
        // Initial pass-through identification, then keep any pass-through that still has a surviving descendant
        // (a blocked OpenL leaf or a non-pass-through sub-aggregator) so the survivor's <parent> stays valid
        // and the participant won't try to scan past its preserved pom.
        var passThroughDirs = identifyPassThroughs(anchorDir);
        var survivorDirs = identifySurvivors(anchorDir, passThroughDirs, blocked);
        retainOnlyDeletable(passThroughDirs, survivorDirs);

        // First pass: assign each leaf to its collapse anchor (today's behaviour). This is the fallback
        // assignment used when the user declines a sub-anchor proposal.
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

        // Conflict detection: for each leaf, walk every parent folder and find a HIGHER-preservation
        // pass-through anchor — i.e. one that keeps the leaf's original groupId where its collapse anchor
        // would shift it. Such pass-throughs are sub-anchor proposals; the user decides whether to promote
        // each (Y default — preserve original coordinates; N — drop the proposal and fall back to the
        // majority/most-compatible flatten at the collapse anchor).
        var subAnchorProposals = proposeSubAnchors(leafToCollapseAnchor, reactorByDir, passThroughDirs, anchorDir);
        var confirmedSubAnchors = promptForSubAnchors(subAnchorProposals);

        // Promoted sub-anchors are removed from the deletion set; they become real anchors.
        for (var subAnchor : confirmedSubAnchors) {
            passThroughDirs.remove(subAnchor.getBasedir().toPath().toAbsolutePath().normalize());
        }

        // Final assignment: a confirmed sub-anchor takes its leaves; everyone else stays at the collapse
        // anchor. Because we removed confirmed sub-anchor dirs from passThroughDirs, collapseAnchorOf now
        // stops at them naturally for any other leaf physically beneath.
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
        // Reconcile <dependenciesThreshold> per anchor across ALL OpenL projects under it (convertible +
        // blocked) — the most permissive value never fails a stricter project that stays classic.
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
     * Walks every parent folder from each leaf up to its current collapse anchor and identifies the highest
     * pass-through ancestor that would preserve the leaf's original groupId. A leaf is treated as
     * <i>unsatisfied</i> when the collapse anchor's heuristic-chosen flatten yields an installed groupId
     * different from the leaf's original — that's the real conflict the user is asked to resolve. Pass-throughs
     * that wouldn't help (no flatten at them yields the original) are not proposed.
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
     * Walks up from {@code leaf}'s parent through the pass-through chain, stopping <i>below</i> the leaf's
     * collapse anchor, and returns the highest pass-through ancestor whose pom would preserve the leaf's
     * original groupId (with either flatten). Returns {@code null} when no such ancestor exists — the leaf
     * truly can't keep its coordinates without a sub-anchor.
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
     * Asks the user (Y/n, default Y) whether to promote each proposed sub-anchor. In batch mode (no TTY /
     * {@code -B}) or when the prompter is unavailable, defaults to Y — the original-coordinate-preserving
     * choice. A leaf that opts out (user picks N) stays at its collapse anchor and inherits the
     * majority/most-compatible flatten choice there (its installed groupId will shift).
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
     * Survivors = reactor projects that keep their {@code pom.xml} after migration: ${project} itself, every
     * non-pass-through aggregator under it, and every blocked OpenL leaf. Pass-through aggregators and
     * converted OpenL leaves are <i>not</i> survivors (their poms are deleted).
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
     * Drops from {@code passThroughDirs} any pass-through aggregator that has a survivor strictly under it —
     * deleting it would orphan the survivor's {@code <parent>} chain, and its preserved pom would block the
     * participant from scanning past it. Such a pass-through stays in place and becomes an effective anchor.
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
     * Compatibility heuristic: sets {@code <flattenGroupId>true</>} on an anchor only when more of its leaves'
     * <i>original</i> groupIds match the anchor's groupId verbatim than match the default path-derived form
     * ({@code anchor.groupId + ".<dotted-path>"}). For corporate repos where every OpenL leaf inherits the
     * same groupId from a shared parent, this picks the flat form so installed coordinates don't shift after
     * the collapse. When the repo encodes the directory path into per-leaf groupIds, the heuristic leaves
     * the default (path-derived) form intact. Returns {@code false} on a tie — the default needs no extra
     * configuration on the anchor.
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
     * Walks up from {@code projectDir} through <i>pass-through</i> aggregators (poms whose only content is
     * structural — no build, deps, profiles, properties, dependencyManagement, etc.) and returns the first
     * non-pass-through ancestor reactor project. {@code ${project}} is always treated as a valid anchor even
     * when pass-through, because we cannot reach above the invocation root. Pass-through aggregators walked
     * past are added to {@code collapsedOut} so the caller deletes them — flattening the tree into a single
     * surviving anchor that the participant can scan to discover every {@code rules.xml} folder. Returns
     * {@code null} when no ancestor reactor pom exists.
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
     * Pass-through aggregator detection — packaging {@code pom} whose body is purely structural
     * (parent/GAV/modules/metadata) with nothing that would influence the build: no plugins (or
     * pluginManagement plugins), no profiles, no properties, no dependencies, no dependencyManagement entries,
     * no repositories or pluginRepositories, no reporting, no distributionManagement, no custom source/resource
     * directories or finalName. These poms exist only to organise the directory tree and can be safely removed
     * when collapsing the migration into a single top-level anchor.
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
     * A single anchor pom to edit: where the plugin/threshold/hoist land, and its convertible members.
     * {@code flattenGroupId} is {@code true} when at least one of the anchor's leaves reached it through a
     * pass-through aggregator that was collapsed — those leaves would otherwise pick up the dotted
     * intermediate path in their derived groupId, so the flag keeps coordinates stable across the collapse.
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
        // Pin a version when the anchor didn't already declare one — the participant binds the synthesised
        // pom-less projects to the anchor plugin's version. Never overrides an explicit version.
        if (plugin.getVersion() == null && pluginVersion != null) {
            plugin.setVersion(pluginVersion);
        }
        if (maxThreshold != null) {
            setConfigChild(plugin, PackageMojo.DEPENDENCIES_THRESHOLD_PARAM, Integer.toString(maxThreshold));
        }
        if (flattenGroupId) {
            // Pass-throughs were collapsed under this anchor; tell the participant to use the anchor's groupId
            // verbatim for every pom-less leaf below it (skip the path-based dotted derivation) so the leaves'
            // installed coordinates don't change because of the collapse.
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
