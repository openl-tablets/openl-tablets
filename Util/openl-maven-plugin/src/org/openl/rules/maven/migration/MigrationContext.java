package org.openl.rules.maven.migration;

import java.util.Collection;
import java.util.function.Supplier;

import org.openl.message.OpenLMessage;

/**
 * What a {@link Migrator} can learn about the project it migrates. Each value is computed only when a migrator asks
 * for it.
 *
 * @param generatedInterface  the interface OpenL generates for the project as it was before the migration
 * @param compilationMessages compiles the project as it is in the folder at the moment of the call and returns the
 *                            messages of the compilation, its errors included
 * @author Yury Molchan
 */
public record MigrationContext(Supplier<Class<?>> generatedInterface,
                               Supplier<Collection<OpenLMessage>> compilationMessages) {
}
