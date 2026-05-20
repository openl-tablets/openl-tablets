package org.openl.rules.maven.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

/**
 * In-memory {@link ModelSource2} that lets {@code ProjectBuilder} consume a synthesised
 * {@link Model} without ever writing it to disk. The reported location points at the project's
 * {@code rules.xml} so error messages remain meaningful and Maven's location-tracking attributes
 * each model element to the right source.
 *
 * @author Yury Molchan
 */
final class InMemoryModelSource implements ModelSource2 {

    private final byte[] xmlBytes;
    private final Path location;

    InMemoryModelSource(Model model, Path location) throws IOException {
        var out = new ByteArrayOutputStream();
        new MavenXpp3Writer().write(out, model);
        this.xmlBytes = out.toByteArray();
        this.location = location;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(xmlBytes);
    }

    @Override
    public String getLocation() {
        return location.toString();
    }

    @Override
    public URI getLocationURI() {
        return location.toUri();
    }

    @Override
    public ModelSource2 getRelatedSource(String relPath) {
        return null;
    }
}
