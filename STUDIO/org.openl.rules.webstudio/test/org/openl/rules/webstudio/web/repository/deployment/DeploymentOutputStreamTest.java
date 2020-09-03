package org.openl.rules.webstudio.web.repository.deployment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;

public class DeploymentOutputStreamTest {

    private Manifest manifest;

    @Before
    public void setUp() {
        manifest = new DeploymentManifestBuilder()
                .setBuildBranch("master")
                .setBuiltBy("John Smith")
                .setImplementationTitle("My Rules")
                .setImplementationVersion("DEFAULT_2020-11-12_11-11-11")
                .setBuildNumber("eqsdsdsd1212sds")
                .build();
    }

    @Test
    public void testNullableManifest() throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(makeZip(null)))) {
            ZipEntry fileEntry = zipIn.getNextEntry();
            assertNotNull(fileEntry);
            assertEquals("Main.xlsx", fileEntry.getName());
            assertNull(zipIn.getNextEntry());
        }
    }

    @Test
    public void testNonNullManifest() throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(makeZip(manifest)))) {
            ZipEntry manifestEntry = zipIn.getNextEntry();
            assertNotNull(manifestEntry);
            assertEquals(JarFile.MANIFEST_NAME, manifestEntry.getName());
            Manifest actual = new Manifest(zipIn);
            assertEquals(manifest, actual);

            ZipEntry fileEntry = zipIn.getNextEntry();
            assertNotNull(fileEntry);
            assertEquals("Main.xlsx", fileEntry.getName());
            assertNull(zipIn.getNextEntry());
        }
    }

    private static byte[] makeZip(Manifest manifest) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new DeploymentOutputStream(out, manifest)) {
            zipOut.putNextEntry(new ZipEntry("Main.xlsx"));
            zipOut.write(new byte[0]);
            zipOut.closeEntry();
            zipOut.finish();
        }
        return out.toByteArray();
    }
}
