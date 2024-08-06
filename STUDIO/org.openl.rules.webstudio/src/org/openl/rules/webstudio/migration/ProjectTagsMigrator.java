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
    private final DesignTimeRepository designTimeRepository;

    public ProjectTagsMigrator(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }


    public void migrate(String repositoryId, String projectPath, Map<String, String> projectTags, 
                        CommonUser migrationUser) throws IOException, ProjectException {
        Repository repository = designTimeRepository.getRepository(repositoryId);

        if (repository != null) {
            if (repository instanceof RepositoryDelegate) {
                repository = ((RepositoryDelegate) repository).getOriginal();
            }
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                PropertiesUtils.store(byteArrayOutputStream, projectTags.entrySet());
                try (var inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                    saveTagsForProject(projectPath, repository, inputStream, migrationUser);
                }
            } 
        }
        
    }

    private static void saveTagsForProject(String projectPath, Repository repository, ByteArrayInputStream tagsStream, CommonUser migrationUser) throws IOException, ProjectException {
        FileData projectFolder = repository.check(projectPath);
        if (projectFolder != null) {
            AProject sourceProject = new AProject(repository, projectFolder);
            File tempFolder = null;
            try {
                // Unpack to temp folder
                tempFolder = Files.createTempDirectory("openl").toFile();
                try (FileSystemRepository tempRepository = new FileSystemRepository()) {
                    tempRepository.setRoot(tempFolder);
                    tempRepository.initialize();
                    AProject tempProject = new AProject(tempRepository, sourceProject.getBusinessName());
                    tempProject.update(sourceProject, migrationUser);
                    if (tempProject.hasArtefact(ProjectTags.TAGS_FILE_NAME)) {
                        AProjectResource artefact = (AProjectResource) tempProject.getArtefact(ProjectTags.TAGS_FILE_NAME);
                        artefact.setContent(tagsStream);
                    } else {
                        tempProject.addResource(ProjectTags.TAGS_FILE_NAME, tagsStream);
                    }
                    if (sourceProject.getFileData() != null) {
                        sourceProject.getFileData().setComment("Project tags were moved to tags.properties file");
                    }
                    sourceProject.update(tempProject, migrationUser);
                }
            } finally {
                FileUtils.deleteQuietly(tempFolder);
            }
        }
    }
}
