package org.openl.rules.common.impl;

import lombok.Builder;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.project.abstraction.RulesProject;

@Builder
public record ProjectDescriptorImpl(
        String repositoryId,
        String projectName,
        String path,
        String branch,
        CommonVersion projectVersion
) implements ProjectDescriptor {

    public static ProjectDescriptor from(RulesProject project) {
        return ProjectDescriptorImpl.builder()
                .repositoryId(project.getRepository().getId())
                .projectName(project.getBusinessName())
                .path(project.getRealPath())
                .projectVersion(project.getVersion())
                .branch(project.getBranch())
                .build();
    }
}
