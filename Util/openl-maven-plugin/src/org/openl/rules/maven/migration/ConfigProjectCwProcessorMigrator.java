package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.project.migration.RulesXmlMigrations;

/**
 * {@code rules.xml} migration: drops the legacy
 * {@code org.openl.rules.project.resolving.CWPropertyFileNameProcessor} reference. Any other custom
 * {@code <properties-file-name-processor>} is preserved. The transform is shared with OpenL Studio in
 * {@link RulesXmlMigrations#cwProcessor}.
 * <p>
 * Migrator id: {@code config.project.cw-processor}.
 *
 * @author Yury Molchan
 */
public final class ConfigProjectCwProcessorMigrator implements Migrator {

    @Override
    public String getId() {
        return "config.project.cw-processor";
    }

    @Override
    public String getCommitMessage() {
        return "drop CWPropertyFileNameProcessor";
    }

    @Override
    public String getDescription() {
        return """
                Removes the discontinued org.openl.rules.project.resolving.CWPropertyFileNameProcessor
                reference from rules.xml's <properties-file-name-processor>. Any other custom processor
                class is preserved — only the specific CW class is dropped.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        return ConfigProjectIO.roundtrip(this, sourceFolder, RulesXmlMigrations::cwProcessor);
    }
}
