package org.openl.rules.ruleservice.deployer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;

/**
 * Deploys an archive nested into a Spring Boot executable jar, through the file system Spring Boot provides for it.
 *
 * @author Yury Molchan
 */
class RulesDeployerServiceNestedArchiveTest {

    @TempDir
    private Path tempDir;

    @Test
    void deploysArchiveNestedIntoSpringBootJar() throws Exception {
        var rules = new LinkedHashMap<String, byte[]>();
        rules.put("rules.xml", bytes("<project><name>sample</name></project>"));
        rules.put("Sample.xlsx", bytes("not a real workbook"));
        var rulesJar = zip(rules);
        // Spring Boot stores the nested jars of an executable jar without compression
        var app = Files.write(tempDir.resolve("app.jar"), zip(Map.of("BOOT-INF/lib/rules.jar", rulesJar)));
        var nested = Path.of(URI.create("nested:" + app.toUri().getRawPath() + "/!BOOT-INF/lib/rules.jar"));

        var repository = mock(Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        var saved = new ByteArrayOutputStream();
        var name = new String[1];
        doAnswer(invocation -> {
            name[0] = invocation.<FileData>getArgument(0).getName();
            invocation.<InputStream>getArgument(1).transferTo(saved);
            return invocation.getArgument(0);
        }).when(repository).save(any(FileData.class), any(InputStream.class));

        try (var deployer = new RulesDeployerService(repository, "deploy/")) {
            deployer.deploy(nested, true);
        }

        assertEquals("deploy/sample/sample", name[0]);
        assertArrayEquals(rulesJar, saved.toByteArray());
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var entry : entries.entrySet()) {
                var zipEntry = new ZipEntry(entry.getKey());
                var crc = new CRC32();
                crc.update(entry.getValue());
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setSize(entry.getValue().length);
                zipEntry.setCrc(crc.getValue());
                zip.putNextEntry(zipEntry);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
