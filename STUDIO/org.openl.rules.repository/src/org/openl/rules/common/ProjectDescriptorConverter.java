package org.openl.rules.common;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;

/**
* Created by ymolchan on 10/6/2014.
*/
class ProjectDescriptorConverter implements Converter {
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
