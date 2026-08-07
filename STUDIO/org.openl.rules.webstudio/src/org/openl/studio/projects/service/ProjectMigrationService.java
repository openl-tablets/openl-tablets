package org.openl.studio.projects.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.migration.RulesDeployMigrations;
import org.openl.rules.project.migration.RulesXmlMigrations;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.MigrationScope;
import org.openl.studio.projects.model.ProjectMigrationView;
import org.openl.studio.projects.model.ProjectMigrationView.RulesDeploySection;
import org.openl.studio.projects.model.ProjectMigrationView.RulesXmlSection;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.service.files.FileCriteriaQuery;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.util.FileTypeHelper;

/**
 * Migrates a project to the current {@code rules.xml} conventions, the way the {@code openl:migrate} Maven
 * goal does — but from the workspace, over the project file API.
 *
 * <p>A project that has no {@code rules.xml} keeps its workbooks in the root and relies on the resolver to
 * treat each Excel file as a module. Migrating it moves every root workbook — {@code .xls}, {@code .xlsx}
 * and {@code .xlsm} — under {@code rules/} and writes a {@code rules.xml}: an all-{@code .xlsx} project keeps
 * a bare descriptor and relies on the {@code rules/}/{@code tests/} defaults, while a moved {@code .xls} or
 * {@code .xlsm} — which no default matches — makes every workbook a declared module so none is lost. A
 * project that already has a {@code rules.xml} is migrated by running the goal's content migrations and
 * rewriting the file. The method-filter migration runs as the goal's no-compile variant, lifting module
 * filters to a project-level {@code <exposed-methods>}. Unlike the goal, Studio never drops the project
 * {@code <name>}: the goal drops one that repeats the project folder, while in Studio the folder of a
 * mapped repository is not the project's business name, so dropping it would rename the project.
 *
 * <p>A {@link MigrationScope#RULES_DEPLOY} migrate rewrites {@code rules-deploy.xml} the same way, running
 * the deployment content migrations the goal runs.
 *
 * <p>A migrate is offered only when a migration changes what the descriptor <em>declares</em>. The
 * comparison is made between two canonical serializations — the descriptor as read and the descriptor after
 * the migrations — so a file that merely carries an XML prolog, its own indentation, CRLF line endings or a
 * comment is left alone instead of being rewritten for its formatting. Blank values and empty blocks are
 * dropped by that same serialization on both sides, so they are cleaned up whenever the file is written but
 * are no reason to write it; {@code openl:migrate} keeps its {@code config.empty-tag} migrator for them.
 *
 * <p>A rewrite writes the descriptor anew, so comments and layout in the file do not survive it.
 */
@Service
@RequiredArgsConstructor
public class ProjectMigrationService {

    private static final String RULES_XML = ProjectDescriptor.FILE_NAME;
    private static final String RULES_DEPLOY = RulesDeploy.FILE_NAME;
    private static final String RULES_FOLDER = "rules/";

    private final ProjectFilesService filesService;
    private final ProjectFileRootFactory fileRootFactory;

    /**
     * Reports what a migrate would do per scope: the root workbooks it would move when the project has no
     * {@code rules.xml}, whether it would rewrite an existing {@code rules.xml}, and whether it would
     * rewrite the {@code rules-deploy.xml}.
     *
     * <p>A descriptor that cannot be parsed reports nothing to migrate — reading the info must not fail the
     * screen it is read for. The migrate itself says so instead.
     */
    public ProjectMigrationView migrationInfo(RulesProject project) {
        var root = fileRootFactory.of(project);
        var rootFiles = rootFiles(root);
        return new ProjectMigrationView(rulesXmlSection(root, rootFiles), rulesDeploySection(root, rootFiles));
    }

    private RulesXmlSection rulesXmlSection(FileRoot root, List<FsNode> rootFiles) {
        if (!hasRulesXml(rootFiles)) {
            var movable = movableWorkbooks(rootFiles);
            return new RulesXmlSection(movable, !movable.isEmpty(), List.of());
        }
        var plan = planRulesXml(readFile(root, RULES_XML), root);
        if (plan == null) {
            return new RulesXmlSection(List.of(), false, List.of());
        }
        // newModules records why a change that widens the module set is refused, so the UI can explain it
        // instead of offering a doomed migrate.
        return new RulesXmlSection(List.of(), plan.migratable(), plan.newModules);
    }

    private RulesDeploySection rulesDeploySection(FileRoot root, List<FsNode> rootFiles) {
        var plan = hasFile(rootFiles, RULES_DEPLOY) ? planRulesDeploy(readFile(root, RULES_DEPLOY)) : null;
        return new RulesDeploySection(plan != null && plan.migratable());
    }

    /**
     * Migrates the requested scope. {@link MigrationScope#RULES_XML} moves the root workbooks under
     * {@code rules/} and writes a {@code rules.xml} when the project has none, otherwise rewrites the
     * existing {@code rules.xml} to its minimal form. {@link MigrationScope#RULES_DEPLOY} rewrites the
     * {@code rules-deploy.xml}, or does nothing when the project has none.
     *
     * <p>A scope the migrations leave unchanged is not written, so nothing happens for a project that has
     * nothing to migrate. A descriptor that cannot be parsed is reported as a conflict.
     */
    public void migrate(RulesProject project, MigrationScope scope) {
        var root = fileRootFactory.of(project);
        var rootFiles = rootFiles(root);
        switch (scope) {
            case RULES_XML -> migrateRulesXmlScope(root, rootFiles);
            case RULES_DEPLOY -> migrateRulesDeploy(root, rootFiles);
        }
    }

    private void migrateRulesXmlScope(FileRoot root, List<FsNode> rootFiles) {
        if (!hasRulesXml(rootFiles)) {
            migrateRootWorkbooks(root, rootFiles);
            return;
        }
        migrateDescriptor(root, RULES_XML, planRulesXml(readFile(root, RULES_XML), root));
    }

    /**
     * Writes the migrated descriptor, and only when a migration changes what it declares — a file that
     * differs from the canonical form by its formatting alone is left as the author wrote it, matching the
     * {@code migratable} flag the caller was offered the migrate by.
     */
    private void migrateDescriptor(FileRoot root, String name, @Nullable DescriptorPlan plan) {
        if (plan == null) {
            throw new ConflictException("projects.migration.unreadable.message", name);
        }
        if (!plan.newModules.isEmpty()) {
            // Collapsing a module to a folder wildcard would turn undeclared workbooks into modules. There is
            // no minimal form that keeps the behaviour, so the migrate is refused rather than silently widening.
            throw new ConflictException("projects.migration.widens.message", String.join(", ", plan.newModules));
        }
        var migrated = plan.migrated;
        if (migrated != null) {
            filesService.updateResource(root, name, new ByteArrayInputStream(migrated));
        }
    }

    private void migrateRootWorkbooks(FileRoot root, List<FsNode> rootFiles) {
        var movable = movableWorkbooks(rootFiles);
        if (movable.isEmpty()) {
            // Nothing to move, so nothing to migrate — matches migrationInfo()'s migratable flag, and
            // avoids writing a rules.xml that would switch the project off SimpleXlsResolvingStrategy
            // for no reason.
            return;
        }
        for (var path : movable) {
            filesService.moveResource(root, path, RULES_FOLDER + path);
        }
        filesService.createResource(root, RULES_XML, new ByteArrayInputStream(rulesXmlForMovedWorkbooks(movable)), false);
    }

    /**
     * The {@code rules.xml} to write for the moved workbooks. The {@code rules/**}{@code /*.xlsx} and
     * {@code tests/**}{@code /*.xlsx} defaults match the moved {@code .xlsx} files, so an all-{@code .xlsx}
     * project stays a bare descriptor whose folder is its identity — nothing a later migrate would strip. A
     * {@code .xls} or {@code .xlsm} file no default matches, so once any is moved every workbook is declared
     * explicitly to keep them all as modules.
     */
    private static byte[] rulesXmlForMovedWorkbooks(List<String> movable) {
        var descriptor = new ProjectDescriptor();
        if (movable.stream().allMatch(path -> path.toLowerCase(Locale.ROOT).endsWith(".xlsx"))) {
            return descriptor.toBytes();
        }
        var modules = new ArrayList<Module>(movable.size());
        for (var path : movable) {
            var module = new Module();
            module.setRulesRootPath(RULES_FOLDER + path);
            modules.add(module);
        }
        descriptor.setModules(modules);
        return descriptor.toBytes();
    }

    /**
     * Plans the {@code rules.xml} migration: parses the file, applies the content migrations in place, and
     * reports what the descriptor declares today, what a migrate would write, and the workbooks the change
     * would turn into modules. A non-empty {@code newModules} means the migrate would widen the module set
     * and must be refused. Returns {@code null} when the file is not a descriptor that can be read.
     *
     * <p>The widening check — and the recursive file listing it needs — is skipped when the migration leaves
     * the {@code <modules>} declarations untouched, since the module set then cannot have grown. When they do
     * change, files are listed recursively so an undeclared workbook nested under {@code rules/} or
     * {@code tests/} is seen.
     */
    private @Nullable DescriptorPlan planRulesXml(byte[] original, FileRoot root) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(original));
        if (descriptor == null) {
            return null;
        }
        var declared = descriptor.toBytes();
        var modulesBefore = declaredModulePaths(descriptor);
        RulesXmlMigrations.apply(descriptor);
        var migrated = descriptor.toBytes();
        if (modulesBefore.equals(declaredModulePaths(descriptor))) {
            return DescriptorPlan.of(declared, migrated);
        }
        var files = projectFiles(allFiles(root));
        var before = RulesXmlMigrations.resolveModuleWorkbooks(
                ProjectDescriptor.read(new ByteArrayInputStream(declared)), files);
        var after = RulesXmlMigrations.resolveModuleWorkbooks(descriptor, files);
        return DescriptorPlan.of(declared, migrated, RulesXmlMigrations.addedWorkbooks(before, after));
    }

    /** The {@code rules-deploy.xml} plan, or {@code null} when the file is not a descriptor that can be read. */
    private static @Nullable DescriptorPlan planRulesDeploy(byte[] original) {
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(original));
        if (rulesDeploy == null) {
            return null;
        }
        var declared = rulesDeploy.toBytes();
        RulesDeployMigrations.apply(rulesDeploy);
        return DescriptorPlan.of(declared, rulesDeploy.toBytes());
    }

    /**
     * What a migrate would do to a descriptor: the bytes it would write, and the workbooks the change would
     * turn into modules.
     *
     * <p>A plan is built from two canonical serializations of the descriptor — as read, and after the
     * migrations. Both come from the same serializer, so it keeps bytes only where a migration changed the
     * content; the formatting of the file on disk plays no part. No bytes means there is nothing to migrate.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class DescriptorPlan {

        /** The bytes a migrate writes, or {@code null} when the migrations changed nothing. */
        private final byte @Nullable [] migrated;
        /** The workbooks the change would turn into modules; empty unless the rewrite widens. */
        private final List<String> newModules;

        static DescriptorPlan of(byte[] declared, byte[] migrated) {
            return of(declared, migrated, List.of());
        }

        static DescriptorPlan of(byte[] declared, byte[] migrated, List<String> newModules) {
            return new DescriptorPlan(Arrays.equals(declared, migrated) ? null : migrated, newModules);
        }

        /** Whether a migration changes what the descriptor declares. */
        boolean migratable() {
            return migrated != null;
        }
    }

    /** The declared module paths, in order — the shape the widening check compares before and after apply. */
    private static List<String> declaredModulePaths(ProjectDescriptor descriptor) {
        var modules = descriptor.getModules();
        return modules == null ? List.of() : modules.stream().map(Module::getRulesRootPath).toList();
    }

    /** Every file in the project, listed recursively. */
    private List<FsNode> allFiles(FileRoot root) {
        return filesService.getResources(root, FileCriteriaQuery.builder().build(), true, FileViewMode.FLAT, null);
    }

    /** The project's file paths, relative to the project root and {@code /}-separated, folders excluded. */
    private static List<String> projectFiles(List<FsNode> files) {
        return files.stream()
                .filter(ProjectMigrationService::isFile)
                .map(FsNode::getPath)
                .filter(Objects::nonNull)
                .toList();
    }

    private void migrateRulesDeploy(FileRoot root, List<FsNode> rootFiles) {
        if (!hasFile(rootFiles, RULES_DEPLOY)) {
            return;
        }
        migrateDescriptor(root, RULES_DEPLOY, planRulesDeploy(readFile(root, RULES_DEPLOY)));
    }

    private byte[] readFile(FileRoot root, String name) {
        try (var content = filesService.getResource(root, name, null).getContent()) {
            return content.readAllBytes();
        } catch (ProjectException | IOException e) {
            throw new ConflictException("file.update.failed.message");
        }
    }

    private List<FsNode> rootFiles(FileRoot root) {
        return filesService.getResources(root, FileCriteriaQuery.builder().build(), false, FileViewMode.FLAT, null);
    }

    private static boolean hasRulesXml(List<FsNode> rootFiles) {
        return hasFile(rootFiles, RULES_XML);
    }

    private static boolean hasFile(List<FsNode> rootFiles, String name) {
        return rootFiles.stream().anyMatch(node -> isFile(node) && name.equals(node.getName()));
    }

    private static List<String> movableWorkbooks(List<FsNode> rootFiles) {
        return rootFiles.stream()
                .filter(ProjectMigrationService::isRootWorkbook)
                .map(FsNode::getPath)
                .sorted()
                .toList();
    }

    private static boolean isRootWorkbook(FsNode node) {
        var path = node.getPath();
        // Every Excel workbook (.xls/.xlsx/.xlsm) is a module in a project without rules.xml, so migrating
        // must move them all — matching the resolver's FileTypeHelper.isExcelFile, not just .xlsx.
        return isFile(node)
                && path != null
                && !path.contains("/")
                && FileTypeHelper.isExcelFile(path);
    }

    private static boolean isFile(FsNode node) {
        return node instanceof FileNode;
    }
}
