package org.openl.studio.projects.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.migration.RulesDeployMigrations;
import org.openl.rules.project.migration.RulesXmlMigrations;
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
 * and {@code .xlsm} — under {@code rules/} and writes a {@code rules.xml}, so the {@code rules/}/{@code tests/}
 * Excel defaults now match them — writing the {@code rules.xml} without moving them first would lose them. A project that already has a
 * {@code rules.xml} is migrated by running the same content migrations the goal runs and rewriting the
 * file. The method-filter migration runs as the goal's no-compile variant, lifting module filters to a
 * project-level {@code <exposed-methods>}.
 *
 * <p>A {@link MigrationScope#RULES_DEPLOY} migrate rewrites {@code rules-deploy.xml} the same way — the
 * deployment content migrations the goal runs, plus the empty-tag cleanup that re-serialization does for
 * free.
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
        var original = readFile(root, RULES_XML);
        var plan = planRulesXml(original, root);
        // migratable keeps its meaning — the file would change; newModules records why a change that widens
        // the module set is refused, so the UI can explain it instead of offering a doomed migrate.
        return new RulesXmlSection(List.of(), changed(original, plan.migrated()), plan.newModules());
    }

    private RulesDeploySection rulesDeploySection(FileRoot root, List<FsNode> rootFiles) {
        return new RulesDeploySection(wouldRewriteRulesDeploy(root, rootFiles));
    }

    /**
     * Migrates the requested scope. {@link MigrationScope#RULES_XML} moves the root workbooks under
     * {@code rules/} and writes a {@code rules.xml} when the project has none, otherwise rewrites the
     * existing {@code rules.xml} to its minimal form. {@link MigrationScope#RULES_DEPLOY} rewrites the
     * {@code rules-deploy.xml}, or does nothing when the project has none.
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
        var original = readFile(root, RULES_XML);
        var plan = planRulesXml(original, root);
        if (!plan.newModules().isEmpty()) {
            // Collapsing a module to a folder wildcard would turn undeclared workbooks into modules. There is
            // no minimal form that keeps the behaviour, so the migrate is refused rather than silently widening.
            throw new ConflictException("projects.migration.widens.message", String.join(", ", plan.newModules()));
        }
        writeIfChanged(root, RULES_XML, original, plan.migrated());
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
        // A bare rules.xml with no <name> and no <modules>: the folder is the project's identity, and the
        // rules/**+tests/** defaults now match the moved workbooks. Nothing is added that a later migrate
        // would then strip.
        filesService.createResource(root, RULES_XML, new ByteArrayInputStream(new ProjectDescriptor().toBytes()), false);
    }

    /**
     * Parses {@code rules.xml} once, resolves the module set it declares, applies the content migrations in
     * place, resolves the set again, and reports the migrated bytes plus the workbooks the change would add.
     * A non-empty {@code newModules} means the migrate would widen the module set and must be refused. Files
     * are listed recursively, so an undeclared workbook nested under {@code rules/} or {@code tests/} is seen.
     */
    private RulesXmlPlan planRulesXml(byte[] original, FileRoot root) {
        var descriptor = ProjectDescriptor.read(new ByteArrayInputStream(original));
        var files = projectFiles(allFiles(root));
        var before = RulesXmlMigrations.resolveModuleWorkbooks(descriptor, files);
        RulesXmlMigrations.apply(descriptor);
        var after = RulesXmlMigrations.resolveModuleWorkbooks(descriptor, files);
        return new RulesXmlPlan(descriptor.toBytes(), RulesXmlMigrations.addedWorkbooks(before, after));
    }

    private record RulesXmlPlan(byte[] migrated, List<String> newModules) {
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
        var original = readFile(root, RULES_DEPLOY);
        writeIfChanged(root, RULES_DEPLOY, original, migratedRulesDeploy(original));
    }

    private boolean wouldRewriteRulesDeploy(FileRoot root, List<FsNode> rootFiles) {
        if (!hasFile(rootFiles, RULES_DEPLOY)) {
            return false;
        }
        var original = readFile(root, RULES_DEPLOY);
        return changed(original, migratedRulesDeploy(original));
    }

    /** Writes the migrated bytes back only when the transform actually changed the file. */
    private void writeIfChanged(FileRoot root, String name, byte[] original, byte @Nullable [] migrated) {
        if (changed(original, migrated)) {
            filesService.updateResource(root, name, new ByteArrayInputStream(migrated));
        }
    }

    private static boolean changed(byte[] original, byte @Nullable [] migrated) {
        return migrated != null && !Arrays.equals(original, migrated);
    }

    /** The {@code rules-deploy.xml} bytes after the content migrations, or {@code null} when it cannot be read. */
    private static byte @Nullable [] migratedRulesDeploy(byte @Nullable [] original) {
        if (original == null) {
            return null;
        }
        var rulesDeploy = RulesDeploy.read(new ByteArrayInputStream(original));
        if (rulesDeploy == null) {
            return null;
        }
        RulesDeployMigrations.apply(rulesDeploy);
        return rulesDeploy.toBytes();
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
