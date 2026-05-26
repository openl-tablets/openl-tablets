package org.openl.rules.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;

import org.openl.rules.maven.migration.Migrator;

/**
 * Lists every available {@code openl:migrate} migrator id (sorted alphabetically) together with the commit
 * message each one produces. A quick reference for the {@code -Dopenl.migrate.migrators=...} selector and
 * for {@code mvn openl:help -Dgoal=migrate -Ddetail}.
 * <p>
 * Does not require a project, so it can be run anywhere:
 * <pre>mvn org.openl.rules:openl-maven-plugin:migrate-list</pre>
 * or, when openl-maven-plugin is already on the build, simply:
 * <pre>mvn openl:migrate-list</pre>
 *
 * @author Yury Molchan
 * @since 6.1.0
 */
@Mojo(name = "migrate-list", threadSafe = true, requiresProject = false)
public final class MigrateListMojo extends AbstractMojo {

    @Override
    public void execute() {
        var migrators = MigrateMojo.allMigratorsAlphabetical();
        var log = getLog();
        log.info("Available OpenL migrators (alphabetical):");
        log.info("");
        for (Migrator m : migrators) {
            log.info("  " + m.getId() + "  —  " + m.getCommitMessage());
            // Each migrator authors its description as a text block — both line breaks AND the leading
            // indent (six spaces per line) are baked in, so String#lines() preserves the full layout
            // and no extra padding is needed here.
            m.getDescription().lines().forEach(log::info);
            log.info("");
        }
        log.info("Pass any id on the command line with -Dopenl.migrate.migrators=<id>, or use a category");
        log.info("prefix such as 'config.deploy' to match every config.deploy.* migrator. Multiple ids may");
        log.info("be comma-separated: -Dopenl.migrate.migrators=config.empty-tag,groovy.jakarta.");
    }
}
