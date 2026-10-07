package org.openl.studio.projects.service;

import jakarta.annotation.Nonnull;

import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.util.FileUtils;

@Component
public class ProjectIdentifierMapperImpl implements ProjectIdentifierMapper {

    @Override
    @Nonnull
    public ProjectIdModel map(@Nonnull AProject project) {
        return ProjectIdModel.builder()
                .repository(project.getRepository().getId())
                .projectName(resolveProjectName(project))
                .build();
    }

    protected String resolveProjectName(AProject src) {
        if (src instanceof RulesProject rp) {
            var designRepo = rp.getDesignRepository();
            if (designRepo != null && designRepo.supports().mappedFolders()) {
                // if project repository supports mapped folders, then project id should be based on design folder name
                // it's required to align project id when current project is opened or closed
                // if project is opened its name is different from the name in design repository
                var mappingData = src.getFileData().getAdditionalData(FileMappingData.class);
                if (mappingData != null) {
                    return FileUtils.getName(mappingData.getExternalPath());
                }
            }
        }
        return src.getName();
    }
}
