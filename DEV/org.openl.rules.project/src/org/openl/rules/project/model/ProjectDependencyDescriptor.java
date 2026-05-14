package org.openl.rules.project.model;

import static org.openl.rules.project.model.ProjectDependencyDescriptor.DEPENDENCY_TAG;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;

@XmlRootElement(name = DEPENDENCY_TAG)
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class ProjectDependencyDescriptor {
    static final String DEPENDENCY_TAG = "dependency";
    private String name;
    private boolean autoIncluded;
    private String mavenArtifact;
}
