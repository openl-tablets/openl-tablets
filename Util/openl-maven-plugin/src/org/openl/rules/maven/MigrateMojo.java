package org.openl.rules.maven;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.manager.ScmManager;

import org.openl.OpenClassUtil;
import org.openl.rules.maven.migration.ConfigDeployRuntimeContextMigrator;
import org.openl.rules.maven.migration.ConfigDeployTemplateClassMigrator;
import org.openl.rules.maven.migration.ConfigEmptyTagMigrator;
import org.openl.rules.maven.migration.ConfigProjectClasspathMigrator;
import org.openl.rules.maven.migration.ConfigProjectCwProcessorMigrator;
import org.openl.rules.maven.migration.ConfigProjectDefaultModulesMigrator;
import org.openl.rules.maven.migration.ConfigProjectMethodFilterMigrator;
import org.openl.rules.maven.migration.GroovyJakartaMigrator;
import org.openl.rules.maven.migration.Migrator;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.util.StringUtils;

/**
 * Migrates an OpenL project to the latest version: rules.xml, rules-deploy.xml, groovy scripts. Each migrator
 * that actually changes files produces a separate SCM commit, so the migration history stays granular.
 *
 * @author Yury Molchan
 * @since 6.1.0
 */
@Mojo(name = "migrate", threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public final class MigrateMojo extends BaseOpenLMojo {

    @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
    private List<String> classpath;

    /**
     * Additional options for compilation defined externally, such as external dependencies and overridden
     * system properties.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    /**
     * Ids (or id prefixes) of the migrators to run. Defaults to the synthetic {@code all} id, which
     * matches every registered migrator. Empty/unset is equivalent.
     * <p>
     * Each entry can be:
     * <ul>
     *     <li>{@code all} — the synthetic catch-all id (also the default). Whenever this value appears
     *         anywhere in the list, every registered migrator runs and other entries are ignored.</li>
     *     <li>an exact id — e.g. {@code config.empty-tag}, {@code groovy.jakarta}</li>
     *     <li>a category prefix that matches every migrator whose id starts with {@code <prefix>.}
     *         — e.g. {@code config.deploy} (every {@code config.deploy.*} phase), {@code config}
     *         (every {@code config.*} phase), {@code groovy} (every {@code groovy.*})</li>
     * </ul>
     * On the command line, multiple values are passed comma-separated:
     * {@code -Dopenl.migrate.migrators=config.deploy.drop-rmi,groovy.jakarta}.
     * <p>
     * Run {@code mvn openl:migrate-list} to see every available migrator id (sorted alphabetically)
     * together with its commit message.
     */
    @Parameter(property = "openl.migrate.migrators", defaultValue = "all")
    private List<String> migrators;

    /**
     * Prefix substituted for the {@code @{prefix}} placeholder in {@link #commentTemplate}.
     */
    @Parameter(property = "openl.migrate.commentPrefix", defaultValue = "migrate:")
    private String commentPrefix;

    /**
     * Template for SCM commit messages. Supports three placeholders, all expanded literally
     * (no escaping):
     * <ul>
     *     <li>{@code @{prefix}}  — replaced with {@link #commentPrefix}</li>
     *     <li>{@code @{message}} — replaced with the migrator's {@link Migrator#getCommitMessage()}</li>
     *     <li>{@code @{version}} — replaced with the openl-maven-plugin version</li>
     * </ul>
     */
    @Parameter(property = "openl.migrate.commentTemplate",
            defaultValue = "@{prefix} @{message} for OpenL @{version}\n\nCo-authored-by: openl-maven-plugin:@{version} <openltablets@eisgroup.com>")
    private String commentTemplate;

    /**
     * When {@code true}, applies migrations to the source tree but skips SCM add/commit. Useful for diffing
     * the migration outcome before committing.
     */
    @Parameter(property = "openl.migrate.dryRun", defaultValue = "false")
    private boolean dryRun;

    /**
     * SCM connection URL used to create commits, e.g. {@code scm:git:https://github.com/org/repo.git}.
     * Defaults to the project's {@code <scm><developerConnection>} element.
     */
    @Parameter(property = "openl.migrate.scmConnection", defaultValue = "${project.scm.developerConnection}")
    private String scmConnection;

    @Component
    private ScmManager scmManager;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws Exception {
        var source = Path.of(sourcePath);
        var all = allMigrators();
        var selected = selectMigrators(all, migrators);
        if (selected.isEmpty()) {
            info("No migrator matched '", migrators, "'. Available ids: ", joinIds(all));
            return;
        }
        info("Migrators to run: ", joinIds(selected));
        if (dryRun) {
            info("Dry-run mode: SCM commits are skipped.");
        }

        var urls = toURLs(classpath);
        ClassLoader classLoader = null;
        try {
            classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            var builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            var factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setExecutionMode(true)
                    .setExternalParameters(externalParameters)
                    .build();

            Supplier<Class<?>> generatedInterface = () -> {
                try {
                    return factory.getInterfaceClass();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to obtain generated interface class", e);
                }
            };

            for (Migrator migrator : selected) {
                // A single migrator's failure (broken transform, SCM error, etc.) must not abort the whole
                // run — surviving migrators may still apply useful changes. The error message goes to ERROR,
                // the stacktrace stays at DEBUG so build logs remain readable by default.
                try {
                    List<Path> changed = migrator.migrate(source, generatedInterface);
                    if (changed.isEmpty()) {
                        debug("[", migrator.getId(), "] no changes.");
                        continue;
                    }
                    debug("[", migrator.getId(), "] ", changed.size(), " file(s) changed.");
                    if (!dryRun) {
                        commit(source, changed, migrator);
                    }
                } catch (Exception e) {
                    error("[", migrator.getId(), "] failed: ", e.getMessage());
                    debug(e);
                }
            }
        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
    }

    @Override
    String getHeader() {
        return "OPENL MIGRATE";
    }

    /**
     * Every available migrator, in execution order (this is the order {@code openl:migrate} runs them).
     * Stays private — external callers (the {@code openl:migrate-list} goal, unit tests) use
     * {@link #allMigratorsAlphabetical()} instead, which never depends on the execution-order semantics.
     */
    static List<Migrator> allMigrators() {
        return List.of(
                // Empty-tag cleanup for both rules.xml and rules-deploy.xml — one commit covering both files.
                new ConfigEmptyTagMigrator(),
                // rules.xml: one atomic step per content rewrite.
                new ConfigProjectClasspathMigrator(),
                new ConfigProjectCwProcessorMigrator(),
                new ConfigProjectMethodFilterMigrator(),
                new ConfigProjectDefaultModulesMigrator(),
                // rules-deploy.xml: one atomic step per content rewrite.
                new ConfigDeployRuntimeContextMigrator(),
                new ConfigDeployTemplateClassMigrator(),
                // groovy
                new GroovyJakartaMigrator());
    }

    /**
     * Same migrators as {@link #allMigrators}, but sorted alphabetically by id. Used by the
     * {@code openl:migrate-list} goal so users can discover every available id in a stable order
     * regardless of the internal execution sequence.
     */
    static List<Migrator> allMigratorsAlphabetical() {
        return allMigrators().stream()
                .sorted(Comparator.comparing(Migrator::getId))
                .toList();
    }

    /**
     * Synthetic id that matches every registered migrator. Used as the default value of the
     * {@link #migrators} parameter and recognised wherever a selector is parsed.
     */
    static final String ALL_ID = "all";

    static List<Migrator> selectMigrators(List<Migrator> all, List<String> selectors) {
        if (selectors == null || selectors.isEmpty()) {
            return all;
        }
        // Maven CLI delivers comma-separated -D values as a single-element list — flatten + trim so the
        // selector list semantics are identical whether the user wrote
        //   <migrators><migrator>config.project</migrator><migrator>groovy</migrator></migrators>
        // in pom.xml or -Dopenl.migrate.migrators=config.project,groovy on the command line.
        var ids = selectors.stream()
                .filter(StringUtils::isNotBlank)
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        // No effective selector, or the synthetic "all" id is present — return everything regardless of
        // any other entry. "all" wins over peers so that mixed lists like ["groovy", "all"] still match
        // every migrator, matching the documented contract.
        if (ids.isEmpty() || ids.contains(ALL_ID)) {
            return all;
        }
        var result = new ArrayList<Migrator>();
        for (var migrator : all) {
            if (matches(migrator.getId(), ids)) {
                result.add(migrator);
            }
        }
        return List.copyOf(result);
    }

    private static boolean matches(String migratorId, List<String> selectedIds) {
        for (var id : selectedIds) {
            if (migratorId.equals(id) || migratorId.startsWith(id + ".")) {
                return true;
            }
        }
        return false;
    }

    private static String joinIds(List<Migrator> migrators) {
        return migrators.stream().map(Migrator::getId).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private void commit(Path sourceFolder, List<Path> changed, Migrator migrator) throws Exception {
        if (scmConnection == null || scmConnection.isBlank()) {
            throw new MojoFailureException(
                    "[" + migrator.getId() + "] SCM commit requested but 'scmConnection' (or "
                            + "<project><scm><developerConnection>...) is not configured. "
                            + "Re-run with -Dopenl.migrate.dryRun=true to skip commits.");
        }
        var message = buildCommitMessage(migrator);
        var baseDir = sourceFolder.toFile();
        var files = new ArrayList<File>(changed.size());
        for (var path : changed) {
            files.add(sourceFolder.relativize(path).toFile());
        }
        var fileSet = new ScmFileSet(baseDir, files);
        var repository = scmManager.makeScmRepository(scmConnection);

        AddScmResult addResult = scmManager.add(repository, fileSet);
        if (!addResult.isSuccess()) {
            throw new MojoFailureException("[" + migrator.getId() + "] SCM add failed: "
                    + addResult.getCommandOutput());
        }
        CheckInScmResult checkInResult = scmManager.checkIn(repository, fileSet, message);
        if (!checkInResult.isSuccess()) {
            throw new MojoFailureException("[" + migrator.getId() + "] SCM check-in failed: "
                    + checkInResult.getCommandOutput());
        }
        debug("[", migrator.getId(), "] Committed: ", message);
    }

    String buildCommitMessage(Migrator migrator) {
        return renderCommitTemplate(commentTemplate, commentPrefix, migrator.getCommitMessage(), plugin.getVersion());
    }

    /**
     * Renders {@code @{prefix}}, {@code @{message}} and {@code @{version}} placeholders in
     * {@code template}. Exposed (package-private) for direct testing.
     */
    static String renderCommitTemplate(String template, String prefix, String message, String version) {
        return template
                .replace("@{prefix}", prefix == null ? "" : prefix)
                .replace("@{message}", message == null ? "" : message)
                .replace("@{version}", version == null ? "" : version);
    }

}
