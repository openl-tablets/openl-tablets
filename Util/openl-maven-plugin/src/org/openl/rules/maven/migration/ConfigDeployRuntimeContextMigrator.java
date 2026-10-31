package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.openl.rules.project.migration.RulesDeployMigrations;

/**
 * {@code rules-deploy.xml} migration: drops {@code <isProvideRuntimeContext>false</…>} because that is the
 * runtime default. An explicit {@code true} or {@code null} value is left untouched. The transform is shared
 * with OpenL Studio via {@link RulesDeployMigrations#runtimeContext}.
 * <p>
 * Migrator id: {@code config.deploy.runtime-context}.
 *
 * @author Yury Molchan
 */
public final class ConfigDeployRuntimeContextMigrator extends AbstractMigrator {

    public ConfigDeployRuntimeContextMigrator() {
        super("config.deploy.runtime-context",
                "drop default isProvideRuntimeContext=false",
                """
                Drops the isProvideRuntimeContext element from rules-deploy.xml when its value is false,
                because false is what the service does anyway by default. Explicit true or an absent value
                stay untouched. Services that rely on the "no runtime context" behaviour keep working —
                only the now-redundant line goes away.
                """);
    }

    @Override
    public List<Path> migrate(Path sourceFolder, MigrationContext context)
            throws IOException {
        return ConfigDeployIO.roundtrip(this, sourceFolder, RulesDeployMigrations::runtimeContext);
    }
}
