package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
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
                project folder name.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder, ConfigProjectDefaultModulesMigrator::transform);
    }

    /**
     * The shared module defaults, plus the project-name drop the {@code openl:migrate} goal adds on top —
     * Studio applies {@link RulesXmlMigrations#defaultModules} alone and keeps the name.
     * Package-private for direct unit testing.
     */
    static void transform(ProjectDescriptor descriptor) {
        RulesXmlMigrations.defaultModules(descriptor);
        dropProjectNameWhenEqualsFolder(descriptor);
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
