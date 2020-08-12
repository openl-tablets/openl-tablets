package org.openl.rules.webstudio.web.repository.deployment;

import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.util.io.BufferingOutputStream;

/**
 * Deployment Output Stream with Manifest entry at the first place
 */
public class DeploymentOutputStream extends ZipOutputStream {

    public DeploymentOutputStream(OutputStream out, Manifest manifest) throws IOException {
        super(out);
        if (manifest != null) {
            ZipEntry manEntry = new ZipEntry(JarFile.MANIFEST_NAME);
            putNextEntry(manEntry);
            manifest.write(new BufferingOutputStream(this));
            closeEntry();
        }
    }
}
