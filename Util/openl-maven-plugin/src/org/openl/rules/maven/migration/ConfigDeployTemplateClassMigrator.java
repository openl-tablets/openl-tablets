package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.openl.rules.project.migration.RulesDeployMigrations;

/**
 * {@code rules-deploy.xml} migration: normalises the
 * {@code interceptingTemplateClassName}/{@code annotationTemplateClassName} pair. When the intercepting slot
 * holds a non-blank class name it either replaces an empty annotation slot (move) or is silently dropped
 * (the annotation slot already had a value).
 * <p>
 * Blank inputs are left alone — emptying out the slots is the responsibility of
 * {@link ConfigEmptyTagMigrator}. The transform is shared with OpenL Studio via
 * {@link RulesDeployMigrations#templateClass}.
 * <p>
 * Migrator id: {@code config.deploy.template-class}.
 *
 * @author Yury Molchan
 */
public final class ConfigDeployTemplateClassMigrator implements Migrator {

    @Override
    public String getId() {
        return "config.deploy.template-class";
    }

    @Override
    public String getCommitMessage() {
        return "interceptingTemplateClassName to annotationTemplateClassName";
    }

    @Override
    public String getDescription() {
        return """
                Renames the legacy interceptingTemplateClassName slot in rules-deploy.xml to
                annotationTemplateClassName. When both slots are populated the intercepting value is
                dropped — the annotation slot is treated as the authoritative one. Blank intercepting
                values are left as they are.
                """;
    }

    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface)
            throws IOException {
        return ConfigDeployIO.roundtrip(this, sourceFolder, RulesDeployMigrations::templateClass);
    }
}
