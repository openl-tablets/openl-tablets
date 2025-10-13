/*
 * Copyright © 2024 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 * CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.
 */
package org.openl.rules.webstudio.migration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.PropertiesUtils;

public class ProjectTagsMigrator {
    public static final String MIGRATION_COMMENT = "Tags in project %s were moved to tags.properties file";
    public static final String PATH_SEPARATOR = "/";
    private final DesignTimeRepository designTimeRepository;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTagsMigrator.class);
    

    public ProjectTagsMigrator(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }


    public void migrate(String repositoryId, String projectPath, Map<String, String> projectTags, 
                        UserInfo migrationUserInfo) throws IOException, ProjectException {
        Repository repository = designTimeRepository.getRepository(repositoryId);

        if (repository != null && ! projectTags.isEmpty()) {
            if (repository instanceof RepositoryDelegate repositoryDelegate) {
                repository = repositoryDelegate.getOriginal();
            }
            if (repository.check(projectPath) != null) {
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    PropertiesUtils.store(byteArrayOutputStream, projectTags.entrySet());
                    try (var inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                        if (repository.supports().folders()) {
                            saveTagsInFolder(projectPath, repository, inputStream, migrationUserInfo);
                        } else {
                            saveTagsInArchive(projectPath, repository, inputStream, migrationUserInfo);
                        }
                    }
                }
            } else {
                LOG.info("Skipping tag migration for project {}/{} since it doesn't exists anymore in repository", repository.getName(), projectPath);
            }
        } else {
            if (repository == null) {
                LOG.info("Ignoring migration for project {} since its repository {} does not exist",  projectPath, repositoryId);
            } else {
                LOG.info("Ignoring migration for project {}/{} because there are no tags for it", repository.getName(), projectPath);
            }
        }
        
    }

    private void saveTagsInFolder(String projectPath, Repository repository, ByteArrayInputStream inputStream, UserInfo migrationUserInfo) throws IOException {
        if (! projectPath.endsWith(PATH_SEPARATOR)) {
            projectPath = projectPath + PATH_SEPARATOR;
        }
        String fullTagsFileName = projectPath + ProjectTags.TAGS_FILE_NAME;
        FileData tagsFileData = repository.check(fullTagsFileName);

        //If file already exists, it means migration has already been done.
        if (tagsFileData == null) {
            LOG.info("Saving project tags for project {}/{}", repository.getName(), projectPath);
            tagsFileData = new FileData();
            tagsFileData.setName(fullTagsFileName);
            tagsFileData.setAuthor(migrationUserInfo);
            tagsFileData.setComment(String.format(MIGRATION_COMMENT, projectPath));
            repository.save(tagsFileData, inputStream);
        } else {
            LOG.info("Skipping saving tags for project {}/{} since file {} already exists", repository.getName(), projectPath, ProjectTags.TAGS_FILE_NAME);
        }
    }

    private void saveTagsInArchive(String projectPath, Repository repository, ByteArrayInputStream tagsStream, UserInfo migrationUserInfo) throws IOException, ProjectException {

        try(FileItem projectFileItem = repository.read(projectPath)) {
            FileData projectFileData = projectFileItem.getData();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipInputStream zis = new ZipInputStream(projectFileItem.getStream());
                 ZipOutputStream zos = new ZipOutputStream(out)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals(ProjectTags.TAGS_FILE_NAME)) {
                        LOG.info("Skipping saving tags for project {}/{} since archive entry {} already exists", repository.getName(), projectPath, ProjectTags.TAGS_FILE_NAME);
                        return;
                    }
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    IOUtils.copy(zis, zos);
                    zos.closeEntry();
                    zis.closeEntry();
                }
                ZipEntry tagsEntry = new ZipEntry(ProjectTags.TAGS_FILE_NAME);
                zos.putNextEntry(tagsEntry);
                IOUtils.copy(tagsStream, zos);
                zos.closeEntry();
                projectFileData.setAuthor(migrationUserInfo);
                projectFileData.setSize(out.size());
                LOG.info("Saving archived project {}/{} with tags file", repository.getName(), projectPath);
                repository.save(projectFileData, new ByteArrayInputStream(out.toByteArray()));
            } catch (IOException e) {
                throw new ProjectException(e.getMessage(), e);
            }
        }
    }
}
