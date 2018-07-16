package org.openl.rules.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;

import java.io.InputStream;
import java.util.List;

/**
* Created by ymolchan on 10/6/2014.
*/
public class ProjectDescriptorHelper {
    private static final XStream XSTREAM = new XStream(new DomDriver());
    static {
        XSTREAM.addPermission(NoTypePermission.NONE);
        XSTREAM.alias("descriptors", List.class);
        XSTREAM.alias("descriptor", ProjectDescriptor.class, ProjectDescriptorImpl.class);
        XSTREAM.aliasType("version", CommonVersion.class);
        XSTREAM.registerConverter(new CommonVersionConverter());
        XSTREAM.registerConverter(new ProjectDescriptorConverter());
    }

    public static InputStream serialize(List<ProjectDescriptor> descriptors) {
        String xml = XSTREAM.toXML(descriptors);
        return IOUtils.toInputStream(xml);
    }

    @SuppressWarnings("unchecked")
    public static List<ProjectDescriptor> deserialize(InputStream source) {
        return (List<ProjectDescriptor>) XSTREAM.fromXML(source);
    }
}
