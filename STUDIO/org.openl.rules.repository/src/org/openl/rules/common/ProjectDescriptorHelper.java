package org.openl.rules.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.openl.rules.common.impl.ProjectDescriptorImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
* Created by ymolchan on 10/6/2014.
*/
public class ProjectDescriptorHelper {
    private static final XStream XSTREAM = new XStream(new DomDriver());
    static {
        XSTREAM.alias("descriptors", List.class);
        XSTREAM.alias("descriptor", ProjectDescriptor.class, ProjectDescriptorImpl.class);
        XSTREAM.aliasType("version", CommonVersion.class);
        XSTREAM.registerConverter(new CommonVersionConverter());
        XSTREAM.registerConverter(new ProjectDescriptorConverter());
    }

    public static InputStream serialize(List<ProjectDescriptor> descriptors) {
        String xml = XSTREAM.toXML(descriptors);
        byte[] bytes = xml.getBytes(Charset.forName("UTF-8"));
        return new ByteArrayInputStream(bytes);
    }

    @SuppressWarnings("unchecked")
    public static List<ProjectDescriptor> deserialize(InputStream source) {
        return (List<ProjectDescriptor>) XSTREAM.fromXML(source);
    }
}
