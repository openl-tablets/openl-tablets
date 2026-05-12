package org.openl.rules.maven.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Shared read-transform-write helper used by every {@code rules.xml} migrator
 * ({@link ConfigEmptyTagMigrator} for empty-tag cleanup, plus the {@code config.project.*}
 * content-specific migrators such as {@link ConfigProjectCwProcessorMigrator} and
 * {@link ConfigProjectMethodFilterMigrator}).
 *
 * @author Yury Molchan
 */
@Slf4j
final class ConfigProjectIO {

    private ConfigProjectIO() {
    }

    /**
     * Reads {@code rules.xml}, lets {@code transform} mutate the deserialized model, and writes the file
     * back only when the JAXB-serialized result differs from the original on disk. A {@code Migrate: …}
     * info line is emitted only in that "actually changed" case — silent migrators leave no trace.
     *
     * @return single-element list with the rewritten path when content changed, empty list otherwise
     * (missing file or no-op transformation).
     */
    static List<Path> roundtrip(Migrator migrator, Path sourceFolder, Consumer<ProjectDescriptor> transform)
            throws IOException {
        var descriptor = ProjectDescriptor.read(sourceFolder);
        if (descriptor == null) {
            return List.of();
        }
        transform.accept(descriptor);
        var file = sourceFolder.resolve(ProjectDescriptor.FILE_NAME);
        var marshaled = descriptor.toBytes();
        if (Arrays.equals(Files.readAllBytes(file), marshaled)) {
            return List.of();
        }
        log.info("Migrate: {} ({})", ProjectDescriptor.FILE_NAME, migrator.getCommitMessage());
        Files.write(file, marshaled);
        return List.of(file);
    }
}
