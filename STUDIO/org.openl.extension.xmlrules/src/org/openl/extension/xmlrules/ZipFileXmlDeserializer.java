package org.openl.extension.xmlrules;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.lazy.LazyExtensionModule;
import org.openl.util.IOUtils;

public class ZipFileXmlDeserializer {
    public static final String ENTRY_POINT = "model.xml";

    private final File file;

    public ZipFileXmlDeserializer(String uri) {
        File sourceFile;
        try {
            sourceFile = new File(new URI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        if (!sourceFile.exists()) {
            throw new IllegalArgumentException("File " + uri + " doesn't exist");
        }
        file = sourceFile;
   }

    public ExtensionModule deserialize() {
        return new LazyExtensionModule(file, ENTRY_POINT);
    }

    public File getFile() {
        return file;
    }
}
