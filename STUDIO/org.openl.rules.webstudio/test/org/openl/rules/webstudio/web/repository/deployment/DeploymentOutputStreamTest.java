package org.openl.rules.webstudio.web.repository.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeploymentOutputStreamTest {

    private Manifest manifest;

    @BeforeEach
    void setUp() {
        manifest = new DeploymentManifestBuilder()
                .setBuildBranch("master")
                .setBuiltBy("John Smith")
                .setImplementationTitle("My Rules")
                .setImplementationVersion("DEFAULT_2020-11-12_11-11-11")
                .setBuildNumber("eqsdsdsd1212sds")
                .build();
    }

    @Test
    void testNullableManifest() throws IOException {
        try (var zipIn = new ZipInputStream(new ByteArrayInputStream(makeZip(null)))) {
            var fileEntry = zipIn.getNextEntry();
            assertNotNull(fileEntry);
            assertEquals("Main.xlsx", fileEntry.getName());
            assertNull(zipIn.getNextEntry());
        }
    }

    @Test
    void testNonNullManifest() throws IOException {
        try (var zipIn = new ZipInputStream(new ByteArrayInputStream(makeZip(manifest)))) {
            var manifestEntry = zipIn.getNextEntry();
            assertNotNull(manifestEntry);
            assertEquals(JarFile.MANIFEST_NAME, manifestEntry.getName());
            var actual = new Manifest(zipIn);
            assertEquals(manifest, actual);

            var fileEntry = zipIn.getNextEntry();
            assertNotNull(fileEntry);
            assertEquals("Main.xlsx", fileEntry.getName());
            assertNull(zipIn.getNextEntry());
        }
    }

    private static byte[] makeZip(Manifest manifest) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zipOut = new DeploymentOutputStream(out, manifest)) {
            zipOut.putNextEntry(new ZipEntry("Main.xlsx"));
            zipOut.write(new byte[0]);
            zipOut.closeEntry();
            zipOut.finish();
        }
        return out.toByteArray();
    }
}
