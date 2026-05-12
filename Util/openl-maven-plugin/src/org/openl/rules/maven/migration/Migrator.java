package org.openl.rules.maven.migration;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

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
     * Applies the migration to the given source folder.
     *
     * @param sourceFolder       OpenL project root.
     * @param generatedInterface lazy supplier of the compiled OpenL interface class. Only invoked by
     *                           migrators that need it (notably {@link ConfigProjectMethodFilterMigrator}
     *                           when populating {@code <exposed-methods>} from a legacy
     *                           {@code <method-filter>}).
     * @return absolute paths of the files this migrator actually changed. An empty list means the migrator
     * was a no-op for this project and the caller may skip creating an SCM commit.
     */
    List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws Exception;
}
