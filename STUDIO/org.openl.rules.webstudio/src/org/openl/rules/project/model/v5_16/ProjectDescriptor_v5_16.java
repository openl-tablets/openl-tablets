package org.openl.rules.project.model.v5_16;

import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.CLASSPATH_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.DEPENDENCIES_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.DEPENDENCY_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.MODULES_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.MODULE_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.PATH_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.PROJECT_DESCRIPTOR_TAG;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.PROPERTIES_FILE_NAME_PATTERN;
import static org.openl.rules.project.xml.BaseProjectDescriptorSerializer.PROPERTIES_FILE_NAME_PROCESSOR;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.v5_12.ProjectDependencyDescriptor_v5_12;
import org.openl.rules.project.xml.BaseProjectDescriptorSerializer.CollapsedStringAdapter2;


@XmlRootElement(name = PROJECT_DESCRIPTOR_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectDescriptor_v5_16 {
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String name;
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String comment;
    @XmlElement(name = MODULE_TAG)
    @XmlElementWrapper(name = MODULES_TAG)
    private List<Module_v5_16> modules;
    @XmlElementWrapper(name = CLASSPATH_TAG)
    @XmlElement(name = PATH_TAG)
    private List<PathEntry> classpath;

    @XmlElement(name = DEPENDENCY_TAG)
    @XmlElementWrapper(name = DEPENDENCIES_TAG)
    private List<ProjectDependencyDescriptor_v5_12> dependencies;
    @XmlElement(name = PROPERTIES_FILE_NAME_PATTERN)
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String propertiesFileNamePattern;
    @XmlElement(name = PROPERTIES_FILE_NAME_PROCESSOR)
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String propertiesFileNameProcessor;
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String extensionName;

    public String getPropertiesFileNamePattern() {
        return propertiesFileNamePattern;
    }

    public void setPropertiesFileNamePattern(String propertiesFileNamePattern) {
        this.propertiesFileNamePattern = propertiesFileNamePattern;
    }

    public String getPropertiesFileNameProcessor() {
        return propertiesFileNameProcessor;
    }

    public void setPropertiesFileNameProcessor(String propertiesFileNameProcessor) {
        this.propertiesFileNameProcessor = propertiesFileNameProcessor;
    }

    public List<ProjectDependencyDescriptor_v5_12> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ProjectDependencyDescriptor_v5_12> dependencies) {
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Module_v5_16> getModules() {
        return modules;
    }

    public void setModules(List<Module_v5_16> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }
}
