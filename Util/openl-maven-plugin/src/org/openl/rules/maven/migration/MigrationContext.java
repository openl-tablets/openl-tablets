package org.openl.rules.maven.migration;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

/**
 * What a {@link Migrator} can learn about the project it migrates. Each value is computed only when a migrator asks
 * for it.
 *
 * @param generatedInterface  the interface OpenL generates for the project as it was before the migration
 * @param compilation         the messages of the compilation of the project as it is in the folder, its errors
 *                            included
 * @param pom                 where the {@code pom.xml} of the project is; a pom-less project has no file there
 * @param pluginParameters    the names of the {@code openl-maven-plugin} parameters that have an effect
 * @author Yury Molchan
 */
public record MigrationContext(Supplier<Class<?>> generatedInterface,
                               Compilation compilation,
                               Path pom,
                               Set<String> pluginParameters) {
}
