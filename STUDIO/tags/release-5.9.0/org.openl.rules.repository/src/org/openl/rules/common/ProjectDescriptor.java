package org.openl.rules.common;

import java.io.InputStream;
import java.util.List;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDependencyImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

public interface ProjectDescriptor<T extends CommonVersion> {
    public static class ProjectDescriptorConverter implements Converter {
        public boolean canConvert(Class cls) {
            return ProjectDescriptor.class.isAssignableFrom(cls);
        }

        public void marshal(Object arg0, HierarchicalStreamWriter arg1, MarshallingContext arg2) {
            ProjectDescriptor descriptor = (ProjectDescriptor) arg0;
            arg1.startNode("projectName");
            arg1.setValue(descriptor.getProjectName());
            arg1.endNode();
            arg1.startNode("projectVersion");
            arg2.convertAnother(descriptor.getProjectVersion());
            arg1.endNode();
        }

        public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
            arg0.moveDown();
            String projectName = arg0.getValue();
            arg0.moveUp();
            arg0.moveDown();
            CommonVersionImpl projectVersion = (CommonVersionImpl) arg1.convertAnother(null, CommonVersion.class);
            arg0.moveUp();
            return new ProjectDescriptorImpl(projectName, projectVersion);
        }
    }

    public static class ProjectDescriptorHelper {
        private static final XStream XSTREAM = new XStream(new DomDriver());
        static {
            XSTREAM.alias("descriptors", List.class);
            XSTREAM.alias("descriptor", ProjectDescriptor.class, ProjectDescriptorImpl.class);
            XSTREAM.aliasType("version", CommonVersion.class);
            XSTREAM.registerConverter(new CommonVersion.CommonVersionConverter());
            XSTREAM.registerConverter(new ProjectDescriptorConverter());
        }

        public static String serialize(List<ProjectDescriptor> descriptors) {
            return XSTREAM.toXML(descriptors);
        }

        public static List<ProjectDescriptor> deserialize(InputStream source) {
            return (List<ProjectDescriptor>) XSTREAM.fromXML(source);
        }
    }

    String getProjectName();

    T getProjectVersion();

    void setProjectVersion(T version) throws ProjectException;
}
