package org.openl.rules.project.xml.v5_16;

import org.openl.rules.project.model.v5_16.ProjectDescriptor_v5_16;
import org.openl.rules.project.model.v5_16.converter.ProjectDescriptorVersionConverter;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer;

public class XmlProjectDescriptorSerializer_v5_16 extends BaseProjectDescriptorSerializer<ProjectDescriptor_v5_16> {

    public XmlProjectDescriptorSerializer_v5_16() {
        this(true);
    }

    public XmlProjectDescriptorSerializer_v5_16(boolean postProcess) {
        super(postProcess, new ProjectDescriptorVersionConverter(), ProjectDescriptor_v5_16.class);
    }
}
