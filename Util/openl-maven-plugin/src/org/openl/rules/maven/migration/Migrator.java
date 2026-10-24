package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * A single migration step that the {@code openl:migrate} mojo can apply to an OpenL project folder.
 * <p>
 * Migrators are identified by a hierarchical dot-separated id (e.g. {@code groovy.jakarta},
 * {@code config.project}). Users select which migrators to run by listing ids or id prefixes; the prefix
 * matches every migrator whose id starts with {@code <prefix>.}, so {@code groovy} selects every
 * {@code groovy.*} migrator and {@code config} selects every {@code config.*} migrator.
 *
 * @author Yury Molchan
 */
public interface Migrator {

    /**
     * Hierarchical id of this migrator, e.g. {@code groovy.jakarta} or {@code config.project}.
     */
    String getId();

    /**
     * Short commit subject describing what this migrator did, used by {@code openl:migrate} when creating
     * SCM commits.
     */
    String getCommitMessage();

    /**
     * Human-readable explanation of what this migrator does, including the assumptions it relies on and
     * anything that could break for the user (a default behaviour change, a discarded value, a required
     * external state). Surfaced by {@code openl:migrate-list} so users can decide before running.
     * <p>
     * <strong>Length contract:</strong> 10–100 words (most stay well under 50; the headroom is for the
     * few migrators whose behaviour needs careful explanation). Enforced by
     * {@code org.openl.rules.maven.MigratorContractTest#descriptionLengthIsWithinTenToHundredWords}.
     */
    String getDescription();

    /**
     * Applies the migration to the given source folder.
     *
     * @param sourceFolder OpenL project root.
     * @param context      what the migrator can learn about the project: the compiled OpenL interface class, used by
     *                     {@link ConfigProjectMethodFilterMigrator} when populating {@code <exposed-methods>} from a
     *                     legacy {@code <method-filter>}, and the compilation messages of the project as it is on
     *                     disk, used by the {@code rules.*} migrators to find the rules that no longer compile.
     * @return absolute paths of the files this migrator actually changed. An empty list means the migrator
     * was a no-op for this project and the caller may skip creating an SCM commit.
     */
    List<Path> migrate(Path sourceFolder, MigrationContext context) throws IOException;
}
