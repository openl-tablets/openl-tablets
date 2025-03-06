/*
 * Copyright © 2024 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 * CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.
 */
package org.openl.rules.webstudio.migration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.FileUtils;
import org.openl.util.PropertiesUtils;

public class ProjectTagsMigrator {
    private static final String MIGRATION_USERNAME = "webstudio_migration";
    private final DesignTimeRepository designTimeRepository;

    public ProjectTagsMigrator(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }


    public void migrate(String repositoryId, String projectPath, Map<String, String> projectTags) throws IOException, ProjectException {
        Repository repository = designTimeRepository.getRepository(repositoryId);

        if (repository != null) {
            if (repository instanceof RepositoryDelegate) {
                repository = ((RepositoryDelegate) repository).getOriginal();
            }
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                PropertiesUtils.store(byteArrayOutputStream, projectTags.entrySet());
                try (var inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                    saveTagsForProject(projectPath, repository, inputStream);
                }
            } 
        }
        
    }

    private static void saveTagsForProject(String projectPath, Repository repository, ByteArrayInputStream tagsStream) throws IOException, ProjectException {
        FileData projectFolder = repository.check(projectPath);
        AProject sourceProject = new AProject(repository, projectFolder);
        File tempFolder = null;
        try {
            // Unpack to temp folder
            tempFolder = Files.createTempDirectory("openl").toFile();
            try (FileSystemRepository tempRepository = new FileSystemRepository()) {
                tempRepository.setRoot(tempFolder);
                tempRepository.initialize();
                AProject tempProject = new AProject(tempRepository, sourceProject.getBusinessName());
                CommonUser systemUser = createSystemUser();
                tempProject.update(sourceProject, systemUser);
                if (tempProject.hasArtefact(ProjectTags.TAGS_FILE_NAME)) {
                    AProjectResource artefact = (AProjectResource) tempProject.getArtefact(ProjectTags.TAGS_FILE_NAME);
                    artefact.setContent(tagsStream);
                } else {
                    tempProject.addResource(ProjectTags.TAGS_FILE_NAME, tagsStream);
                }
                sourceProject.update(tempProject, systemUser);
            }
        } finally {
            FileUtils.deleteQuietly(tempFolder);
        }
    }

    private static CommonUser createSystemUser() {
        UserInfo systemUserInfo = createSystemUserInfo();
        return new CommonUser() {
            @Override
            public String getUserName() {
                return MIGRATION_USERNAME;
            }

            @Override
            public UserInfo getUserInfo() {
                return systemUserInfo;
            }
        };
    }

    private static UserInfo createSystemUserInfo() {
        return new UserInfo(MIGRATION_USERNAME, "", MIGRATION_USERNAME);
    }
}
