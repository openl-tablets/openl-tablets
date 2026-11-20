package org.openl.rules.webstudio.web.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * What a deployment carries from the Maven repository besides the projects of the design repositories.
 *
 * <p>A library goes to the {@code lib} folder of the project that names it, which the default classpath of a project
 * reads. A file the project holds itself under the same name is kept, and the library is left out. A downloaded
 * project is deployed as a project of its own, as it was unpacked.
 *
 * @param libraries the libraries of each deployed project, by the name of the project
 * @param projects  the dependency projects downloaded from the Maven repository, by name: the folder of each
 * @author Yury Molchan
 */
public record DeploymentExtras(Map<String, List<Path>> libraries, Map<String, Path> projects) {

    public static final DeploymentExtras NONE = new DeploymentExtras(Map.of(), Map.of());

    private static final String LIB = "lib/";

    /** Whether the deployment carries nothing from the Maven repository. */
    public boolean isEmpty() {
        return libraries.values().stream().allMatch(List::isEmpty) && projects.isEmpty();
    }

    /** The libraries of the project, by their path inside the project folder. */
    Map<String, Path> libraryEntries(String project) {
        var entries = new LinkedHashMap<String, Path>();
        for (var library : libraries.getOrDefault(project, List.of())) {
            entries.putIfAbsent(LIB + library.getFileName(), library);
        }
        return entries;
    }

    /** Every file of a downloaded project, by its path inside the project folder. */
    static Map<String, Path> projectEntries(Path folder) {
        try (var files = Files.walk(folder)) {
            var entries = new TreeMap<String, Path>();
            files.filter(Files::isRegularFile)
                    .forEach(file -> entries.put(folder.relativize(file).toString().replace('\\', '/'), file));
            return entries;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The archive of a project with the files added that it does not hold itself.
     *
     * @param archive the archive of the project
     * @param entries the files to add, by their path inside the archive
     */
    static byte[] withEntries(byte[] archive, Map<String, Path> entries) throws IOException {
        if (entries.isEmpty()) {
            return archive;
        }
        var out = new ByteArrayOutputStream();
        var names = new HashSet<String>();
        try (var zipIn = new ZipInputStream(new ByteArrayInputStream(archive));
             var zipOut = new ZipOutputStream(out)) {
            for (var entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                names.add(entry.getName());
                zipOut.putNextEntry(new ZipEntry(entry.getName()));
                zipIn.transferTo(zipOut);
                zipOut.closeEntry();
            }
            for (var entry : entries.entrySet()) {
                if (names.add(entry.getKey())) {
                    zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                    Files.copy(entry.getValue(), zipOut);
                    zipOut.closeEntry();
                }
            }
        }
        return out.toByteArray();
    }

    /** An archive of every file of a folder. */
    static byte[] archiveOf(Path folder) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zipOut = new ZipOutputStream(out)) {
            for (var entry : projectEntries(folder).entrySet()) {
                zipOut.putNextEntry(new ZipEntry(entry.getKey()));
                Files.copy(entry.getValue(), zipOut);
                zipOut.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
