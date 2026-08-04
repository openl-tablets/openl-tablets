package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.project.migration.RulesXmlMigrations;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * {@code rules.xml} migration: drops module and project-name configuration that only restates runtime
 * defaults so the file shrinks to the minimal form that produces the same behaviour — drops a redundant
 * module {@code <name>}, collapses same-folder name-less {@code .xlsx} modules into a
 * {@code <subfolder>}{@code /**}{@code /*.xlsx} wildcard, removes the whole {@code <modules>} block when
 * only the default wildcards remain, and drops the project {@code <name>} when it equals its folder. The
 * transform is shared with OpenL Studio in {@link RulesXmlMigrations#defaultModules}.
 * <p>
 * Migrator id: {@code config.project.default-modules}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectDefaultModulesMigrator implements Migrator {

    @Override
    public String getId() {
        return "config.project.default-modules";
    }

    @Override
    public String getCommitMessage() {
        return "drop redundant module and project-name defaults from rules.xml";
    }

    @Override
    public String getDescription() {
        return """
                Shrinks rules.xml by dropping module names that simply repeat the workbook file name,
                collapsing several same-folder modules into one <subfolder>/**/*.xlsx wildcard, and
                removing the whole <modules> block when only the default rules/**/*.xlsx or
                tests/**/*.xlsx wildcards remain. Also drops the project <name> when it matches the
                project folder name. A module carrying its own configuration is kept, and the rewrite is
                skipped when collapsing would turn an undeclared workbook into a module.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        var files = projectFiles(sourceFolder);
        return ConfigProjectIO.roundtrip(this, sourceFolder, descriptor -> transformAndGuard(descriptor, files));
    }

    /**
     * The shared module defaults plus the project-name drop, refusing the rewrite when it would turn an
     * undeclared workbook into a module.
     *
     * <p>Collapsing modules into folder wildcards changes what compiles when the folder holds workbooks the
     * descriptor did not declare. This resolves the module set against {@code files} before and after the
     * transform and throws when the set grows, so the goal leaves {@code rules.xml} untouched instead of
     * silently widening it. The {@code openl:migrate} mojo logs the refusal and moves on to the next migrator.
     *
     * <p>Package-private for direct unit testing.
     */
    static void transformAndGuard(ProjectDescriptor descriptor, Collection<String> files) {
        var before = RulesXmlMigrations.resolveModuleWorkbooks(descriptor, files);
        transform(descriptor);
        var after = RulesXmlMigrations.resolveModuleWorkbooks(descriptor, files);
        var added = RulesXmlMigrations.addedWorkbooks(before, after);
        if (!added.isEmpty()) {
            throw new IllegalStateException("Refusing to rewrite " + ProjectDescriptor.FILE_NAME
                    + ": it would turn undeclared workbooks into modules (" + String.join(", ", added)
                    + "). Declare or remove them, then migrate.");
        }
    }

    /**
     * The shared module defaults, plus the project-name drop the {@code openl:migrate} goal adds on top —
     * Studio applies {@link RulesXmlMigrations#defaultModules} alone and keeps the name. The pure content
     * transform, without the file-aware widening guard. Package-private for direct unit testing.
     */
    static void transform(ProjectDescriptor descriptor) {
        RulesXmlMigrations.defaultModules(descriptor);
        dropProjectNameWhenEqualsFolder(descriptor);
    }

    /** The project's file paths, relative to the project root and {@code /}-separated. */
    private static Collection<String> projectFiles(Path sourceFolder) throws IOException {
        if (!Files.isDirectory(sourceFolder)) {
            return List.of();
        }
        try (var files = Files.walk(sourceFolder)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> sourceFolder.relativize(path).toString().replace('\\', '/'))
                    .toList();
        }
    }

    private static void dropProjectNameWhenEqualsFolder(ProjectDescriptor descriptor) {
        var projectFolder = descriptor.getProjectFolder();
        if (projectFolder == null) {
            return;
        }
        var folderName = projectFolder.getFileName();
        if (folderName != null && folderName.toString().equals(descriptor.getName())) {
            descriptor.setName(null);
        }
    }
}
