package org.openl.rules.repository.zip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;

/**
 * Tests for the configurable {@code location} search pattern of {@link JarLocalRepository}.
 *
 * @author Yury Molchan
 */
class JarLocalRepositoryTest {

    @TempDir
    private File openlHome;
    @AutoClose
    private Repository repository;

    @Test
    void customLocationDiscoversArchivesOutsideClasspath() throws IOException {
        generateDeployment("singleDeployment.zip",
                Map.of("rules.xml", "foo".getBytes(), "rules/Algorithm.xlsx", "bar".getBytes()));

        var settings = Map.of("location", fileGlob());
        this.repository = new JarRepositoryFactory().create(settings::get);

        Map<String, FileData> files = listRoot();
        assertExists(files.get("singleDeployment/rules.xml"));
        assertExists(files.get("singleDeployment/rules/Algorithm.xlsx"));
        assertArrayEquals("foo".getBytes(), read("/singleDeployment/rules.xml"));
    }

    @Test
    void defaultLocationDoesNotReachExternalArchives() throws IOException {
        generateDeployment("singleDeployment.zip", Map.of("rules.xml", "foo".getBytes()));

        JarLocalRepository repo = new JarLocalRepository();
        repo.setLocation("classpath*:**/*.zip");
        repo.initialize();
        this.repository = repo;

        // The default location pattern searches the classpath, so the archive in the external folder is invisible.
        assertFalse(listRoot().containsKey("singleDeployment/rules.xml"));
    }

    @Test
    void factoryWiresLocationSetting() throws IOException {
        generateDeployment("project.zip", Map.of("rules.xml", "baz".getBytes()));

        var settings = Map.of("location", fileGlob());
        this.repository = new JarRepositoryFactory().create(settings::get);

        assertExists(listRoot().get("project/rules.xml"));
        assertArrayEquals("baz".getBytes(), read("/project/rules.xml"));
    }

    private String fileGlob() {
        return "file:" + openlHome.getAbsolutePath().replace('\\', '/') + "/*.zip";
    }

    private Map<String, FileData> listRoot() throws IOException {
        var map = new HashMap<String, FileData>();
        for (FileData fileData : repository.list("/")) {
            map.put(fileData.getName(), fileData);
        }
        return map;
    }

    private byte[] read(String path) throws IOException {
        FileItem item = repository.read(path);
        assertNotNull(item);
        try (InputStream is = item.getStream()) {
            return is.readAllBytes();
        }
    }

    private void generateDeployment(String name, Map<String, byte[]> entries) throws IOException {
        Path zipFile = openlHome.toPath().resolve(name);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
    }

    private static void assertExists(FileData fileData) {
        assertNotNull(fileData);
        assertNotNull(fileData.getModifiedAt());
    }
}
