package org.openl.rules.maven.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared rewrite helper used by every {@code groovy.*} migrator, such as {@link GroovyJakartaMigrator}. It rewrites
 * every {@code .groovy} file under the project folder whose text the transform changes.
 *
 * @author Yury Molchan
 */
@Slf4j
final class GroovySourcesIO {

    private GroovySourcesIO() {
    }

    /**
     * Rewrites the Groovy sources of the folder with the transform and emits a {@code Migrate …} info line for each
     * rewritten file.
     *
     * @return the rewritten files; empty when no file needed changes or the folder is missing
     */
    static List<Path> rewrite(Path sourceFolder, UnaryOperator<String> transform) throws IOException {
        if (!Files.isDirectory(sourceFolder)) {
            return List.of();
        }
        var changed = new ArrayList<Path>();
        try (var stream = Files.walk(sourceFolder)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".groovy"))
                    .forEach(file -> {
                        if (rewrite(sourceFolder, file, transform)) {
                            changed.add(file);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return List.copyOf(changed);
    }

    /**
     * Returns {@code true} when the file's content was rewritten on disk.
     */
    private static boolean rewrite(Path sourceFolder, Path file, UnaryOperator<String> transform) {
        try {
            var original = Files.readString(file);
            var migrated = transform.apply(original);
            if (migrated.equals(original)) {
                return false;
            }
            log.info("Migrate {}", sourceFolder.relativize(file));
            Files.writeString(file, migrated);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
