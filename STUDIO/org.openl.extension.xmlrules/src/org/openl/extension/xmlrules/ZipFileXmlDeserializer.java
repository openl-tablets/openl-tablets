package org.openl.extension.xmlrules;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.io.IOUtils;
import org.openl.extension.Deserializer;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.single.ExtensionModuleInfo;
import org.openl.extension.xmlrules.model.lazy.LazyExtensionModule;
import org.openl.extension.xmlrules.model.single.SheetInfo;
import org.openl.extension.xmlrules.model.single.*;

public class ZipFileXmlDeserializer implements Deserializer<ExtensionModule> {
    public static final String ENTRY_POINT = "module.xml";

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

    @Override
    public ExtensionModule deserialize(InputStream source) {
        IOUtils.closeQuietly(source); // TODO remove it
        return new LazyExtensionModule(file, ENTRY_POINT);
    }

}
