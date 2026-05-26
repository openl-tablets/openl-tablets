package org.openl.rules.maven.migration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrates Groovy scripts under an OpenL project folder from the legacy {@code javax.*} Java EE namespaces
 * to the Jakarta EE 10 ones. The migrator rewrites the package roots whose entire namespace moved from
 * {@code javax.*} to {@code jakarta.*} in Jakarta EE 9/10 — and that appear (directly or transitively through
 * user beans/services) in OpenL Tablets groovy scripts:
 * <ul>
 *     <li>{@code javax.ws.rs.*} (JAX-RS) → {@code jakarta.ws.rs.*}</li>
 *     <li>{@code javax.xml.bind.*} (JAXB) → {@code jakarta.xml.bind.*}</li>
 *     <li>{@code javax.persistence.*} (JPA) → {@code jakarta.persistence.*}</li>
 *     <li>{@code javax.validation.*} (Bean Validation) → {@code jakarta.validation.*}</li>
 *     <li>{@code javax.servlet.*} → {@code jakarta.servlet.*}</li>
 *     <li>{@code javax.inject.*} → {@code jakarta.inject.*}</li>
 * </ul>
 * Ambiguous prefixes that overlap with Java SE namespaces ({@code javax.annotation.processing},
 * {@code javax.transaction.xa}, {@code javax.security.auth.*}) are intentionally not rewritten.
 * <p>
 * Migrator id: {@code groovy.jakarta}.
 *
 * @author Yury Molchan
 */
@Slf4j
public final class GroovyJakartaMigrator implements Migrator {

    /**
     * Maps the legacy {@code javax.*} package roots used by Jakarta EE in groovy scripts to their Jakarta EE
     * 10 equivalents. {@link LinkedHashMap} preserves a deterministic application order.
     */
    private static final Map<String, String> JAKARTA_NAMESPACE_REWRITES;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("javax.ws.rs.", "jakarta.ws.rs.");
        map.put("javax.xml.bind.", "jakarta.xml.bind.");
        map.put("javax.persistence.", "jakarta.persistence.");
        map.put("javax.validation.", "jakarta.validation.");
        map.put("javax.servlet.", "jakarta.servlet.");
        map.put("javax.inject.", "jakarta.inject.");
        JAKARTA_NAMESPACE_REWRITES = Map.copyOf(map);
    }

    /**
     * Returns {@code true} when the file's content was rewritten on disk.
     */
    private static boolean migrateFile(Path sourceFolder, Path file) {
        try {
            var original = Files.readString(file, StandardCharsets.UTF_8);
            var migrated = migrate(original);
            if (migrated.equals(original)) {
                return false;
            }
            log.info("Migrate {}", sourceFolder.relativize(file));
            Files.writeString(file, migrated, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Applies the {@code javax}→{@code jakarta} rewrites to the given groovy source body. Returns the same
     * instance when nothing changed.
     */
    static String migrate(String source) {
        var result = source;
        for (var entry : JAKARTA_NAMESPACE_REWRITES.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public String getId() {
        return "groovy.jakarta";
    }

    @Override
    public String getCommitMessage() {
        return "groovy scripts from javax to jakarta";
    }

    @Override
    public String getDescription() {
        return """
                Rewrites javax.* imports and references to jakarta.* in every .groovy file under the
                project for the six namespaces that fully moved in Jakarta EE 9/10: ws.rs, xml.bind,
                persistence, validation, servlet, inject. Ambiguous prefixes overlapping with Java SE
                (annotation.processing, transaction.xa, security.auth.*) are left untouched.
                """;
    }

    /**
     * Walks {@code sourceFolder} recursively and rewrites every {@code *.groovy} file in place so that the
     * legacy Jakarta EE {@code javax.*} namespaces are replaced with their Jakarta EE 10 equivalents. Files
     * that do not need changes are left untouched. The {@code generatedInterface} supplier is unused.
     *
     * @return absolute paths of every groovy file whose content was actually rewritten; empty when no file
     * needed changes or the folder is missing.
     */
    @Override
    public List<Path> migrate(Path sourceFolder, Supplier<Class<?>> generatedInterface) throws IOException {
        if (!Files.isDirectory(sourceFolder)) {
            return List.of();
        }
        var changed = new ArrayList<Path>();
        try (Stream<Path> stream = Files.walk(sourceFolder)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".groovy"))
                    .forEach(file -> {
                        if (migrateFile(sourceFolder, file)) {
                            changed.add(file);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return List.copyOf(changed);
    }
}
