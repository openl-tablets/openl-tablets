package org.openl.rules.project.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = XmlProjectDescriptorSerializer.WEBSTUDIO_CONFIGURATION)
public class WebstudioConfiguration {
    private boolean compileThisModuleOnly = false;

    public boolean isCompileThisModuleOnly() {
        return compileThisModuleOnly;
    }

    public void setCompileThisModuleOnly(boolean compileThisModuleOnly) {
        this.compileThisModuleOnly = compileThisModuleOnly;
    }
}
