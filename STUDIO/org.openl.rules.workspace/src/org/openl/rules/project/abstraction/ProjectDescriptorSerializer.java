package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.NoTypePermission;

/**
 * Serializes and deserializes {@link ProjectDescriptor} to/from xml representation
 */
public class ProjectDescriptorSerializer {

    private final XStream xstream;

    public ProjectDescriptorSerializer() {
        xstream = new XStream(new DomDriver());
        xstream.addPermission(NoTypePermission.NONE);
        xstream.ignoreUnknownElements();
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.allowTypeHierarchy(Wrapper.class);
        xstream.allowTypeHierarchy(ProjectDescriptor.class);

        xstream.aliasType("descriptors", Wrapper.class);
        xstream.addImplicitArray(Wrapper.class, "descriptors");
        xstream.aliasType("descriptor", ProjectDescriptor.class);

        xstream.registerConverter(new ProjectDescriptorConverter());
        xstream.registerConverter(new CommonVersionConverter());
    }

    @SuppressWarnings({ "rawtypes" })
    public InputStream serialize(List<ProjectDescriptor> descriptors) {
        return IOUtils.toInputStream(xstream.toXML(new Wrapper(descriptors)));
    }

    @SuppressWarnings({ "rawtypes" })
    public List<ProjectDescriptor> deserialize(InputStream source) {
        try {
            if (source.available() == 0) {
                return null;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return Optional.ofNullable(((Wrapper) xstream.fromXML(source)).descriptors).orElseGet(ArrayList::new);
    }

    /**
     * For internal purpose, to wrap list of elements to the main tag
     */
    @SuppressWarnings({ "rawtypes" })
    private static final class Wrapper {

        public List<ProjectDescriptor> descriptors;

        public Wrapper(List<ProjectDescriptor> descriptors) {
            this.descriptors = descriptors;
        }

        @SuppressWarnings("unused")
        public Wrapper() {
            // Used by XML deserializer
        }
    }

    /**
     * Converts {@link ProjectDescriptor} to the XML representation and vise versa
     */
    private static final class ProjectDescriptorConverter implements Converter {

        @Override
        @SuppressWarnings({ "rawtypes" })
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            ProjectDescriptor pd = (ProjectDescriptor) source;

            addNode("repositoryId", pd.getRepositoryId(), writer, context);
            addNode("path", pd.getPath(), writer, context);
            addNode("projectName", pd.getProjectName(), writer, context);
            addNode("branch", pd.getBranch(), writer, context);
            addNode("projectVersion", pd.getProjectVersion(), writer, context);
        }

        private void addNode(String name, Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
            if (value != null) {
                writer.startNode(name);
                context.convertAnother(value);
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            String repositoryId = null;
            String projectName = null;
            String path = null;
            String branch = null;
            CommonVersion projectVersion = null;
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                String nodeName = reader.getNodeName();
                if ("repositoryId".equals(nodeName)) {
                    repositoryId = readNode(String.class, context);
                } else if ("path".equals(nodeName)) {
                    path = readNode(String.class, context);
                } else if ("projectName".equals(nodeName)) {
                    projectName = readNode(String.class, context);
                } else if ("branch".equals(nodeName)) {
                    branch = readNode(String.class, context);
                } else if ("projectVersion".equals(nodeName)) {
                    projectVersion = readNode(CommonVersion.class, context);
                }
                reader.moveUp();
            }
            return new ProjectDescriptorImpl(repositoryId, projectName, path, branch, projectVersion);
        }

        @SuppressWarnings("unchecked")
        private <T> T readNode(Class<T> type, UnmarshallingContext context) {
            return (T) context.convertAnother(null, type);
        }

        @Override
        public boolean canConvert(Class type) {
            return ProjectDescriptor.class.isAssignableFrom(type);
        }
    }

    /**
     * Converts {@link CommonVersion} to the XML representation and vise versa
     */
    private static final class CommonVersionConverter implements SingleValueConverter {

        @Override
        public String toString(Object source) {
            return ((CommonVersion) source).getVersionName();
        }

        @Override
        public Object fromString(String value) {
            return new CommonVersionImpl(value);
        }

        @Override
        public boolean canConvert(Class type) {
            return CommonVersion.class.isAssignableFrom(type);
        }
    }

}
