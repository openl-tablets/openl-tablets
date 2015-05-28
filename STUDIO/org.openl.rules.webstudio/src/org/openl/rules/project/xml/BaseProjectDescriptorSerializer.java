package org.openl.rules.project.xml;

import java.io.InputStream;
import java.util.ArrayList;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class BaseProjectDescriptorSerializer<T> implements IProjectDescriptorSerializer {
    protected final XStream xstream;
    private final boolean postProcess;
    private final ObjectVersionConverter<ProjectDescriptor, T> projectDescriptorVersionConverter;

    public BaseProjectDescriptorSerializer(boolean postProcess, ObjectVersionConverter<ProjectDescriptor, T> projectDescriptorVersionConverter) {
        this.postProcess = postProcess;
        this.projectDescriptorVersionConverter = projectDescriptorVersionConverter;
        xstream = new XStream(new DomDriver());
    }

    public String serialize(ProjectDescriptor source) {
        return xstream.toXML(projectDescriptorVersionConverter.toOldVersion(source));
    }

    /**
     * @throws com.thoughtworks.xstream.XStreamException if the object cannot be deserialized
     */
    public ProjectDescriptor deserialize(InputStream source) {
        @SuppressWarnings("unchecked")
        T oldVersion = (T) xstream.fromXML(source);
        ProjectDescriptor descriptor = projectDescriptorVersionConverter.fromOldVersion(oldVersion);
        if (postProcess) {
            postProcess(descriptor);
        }
        return descriptor;
    }

    private void postProcess(ProjectDescriptor descriptor) {
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<PathEntry>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<Module>());
        }
    }
}
