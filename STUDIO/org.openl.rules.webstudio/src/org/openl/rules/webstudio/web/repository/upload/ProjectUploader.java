package org.openl.rules.webstudio.web.repository.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermissionsSets;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.FileTypeHelper;

public class ProjectUploader {
    private final String projectName;
    private final String projectFolder;
    private final UserWorkspace userWorkspace;
    private final RepositoryAclService designRepositoryAclService;
    private final PathFilter zipFilter;
    private final List<ProjectFile> uploadedFiles;
    private final ZipCharsetDetector zipCharsetDetector;
    private final String comment;
    private final String repositoryId;
    private final String modelsPath;
    private final String algorithmsPath;
    private final String modelsModuleName;
    private final String algorithmsModuleName;
    private String createdProjectName;

    public ProjectUploader(String repositoryId,
            ProjectFile uploadedFile,
            String projectName,
            String projectFolder,
            UserWorkspace userWorkspace,
            RepositoryAclService designRepositoryAclService,
            String comment,
            PathFilter zipFilter,
            ZipCharsetDetector zipCharsetDetector,
            String modelsPath,
            String algorithmsPath,
            String modelsModuleName,
            String algorithmsModuleName) {
        this.repositoryId = repositoryId;
        this.projectFolder = projectFolder;
        this.comment = comment;
        this.zipCharsetDetector = zipCharsetDetector;
        this.uploadedFiles = new ArrayList<>();
        this.uploadedFiles.add(uploadedFile);

        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
        this.designRepositoryAclService = designRepositoryAclService;
        this.zipFilter = zipFilter;
        this.modelsPath = modelsPath;
        this.algorithmsPath = algorithmsPath;
        this.modelsModuleName = modelsModuleName;
        this.algorithmsModuleName = algorithmsModuleName;
    }

    public ProjectUploader(String repositoryId,
            List<ProjectFile> uploadedFiles,
            String projectName,
            String projectFolder,
            UserWorkspace userWorkspace,
            RepositoryAclService repositoryAclService,
            String comment,
            PathFilter zipFilter,
            ZipCharsetDetector zipCharsetDetector,
            String modelsPath,
            String algorithmsPath,
            String modelsModuleName,
            String algorithmsModuleName) {
        this.repositoryId = repositoryId;
        this.uploadedFiles = uploadedFiles;
        this.projectName = projectName;
        this.projectFolder = projectFolder;
        this.userWorkspace = userWorkspace;
        this.designRepositoryAclService = repositoryAclService;
        this.comment = comment;
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
        this.modelsPath = modelsPath;
        this.algorithmsPath = algorithmsPath;
        this.modelsModuleName = modelsModuleName;
        this.algorithmsModuleName = algorithmsModuleName;
    }

    public RulesProject uploadProject() throws ProjectException {
        AProjectCreator projectCreator = null;
        if (uploadedFiles.isEmpty()) {
            throw new ProjectException("Cannot create project from the given file.");
        }
        try {
            // Get the last file
            ProjectFile file = uploadedFiles.get(uploadedFiles.size() - 1);
            String fileName = file.getName();
            if (FileTypeHelper.isPossibleOpenAPIFile(fileName)) {
                projectCreator = new OpenAPIProjectCreator(file,
                    repositoryId,
                    projectName,
                    projectFolder,
                    userWorkspace,
                    comment,
                    modelsPath,
                    algorithmsPath,
                    modelsModuleName,
                    algorithmsModuleName);
            } else if (FileTypeHelper.isZipFile(fileName)) {
                // Create project creator for the single zip file
                projectCreator = new ZipFileProjectCreator(repositoryId,
                    fileName,
                    file.getInput(),
                    projectName,
                    projectFolder,
                    userWorkspace,
                    comment,
                    zipFilter,
                    zipCharsetDetector);
            } else {
                projectCreator = new ExcelFilesProjectCreator(repositoryId,
                    projectName,
                    projectFolder,
                    userWorkspace,
                    comment,
                    zipFilter,
                    uploadedFiles.toArray(new ProjectFile[0]));
                uploadedFiles.toArray(new ProjectFile[0]);
            }
            RulesProject rulesProject = projectCreator.createRulesProject();
            if (!designRepositoryAclService.createAcl(rulesProject,
                AclPermissionsSets.NEW_PROJECT_PERMISSIONS,
                true)) {
                throw new ProjectException(String.format("Granting permissions to a new project '%s' is failed.",
                    ProjectArtifactUtils.extractResourceName(rulesProject)));
            }
            createdProjectName = projectCreator.getCreatedProjectName();
            // Get just created project, because creator API doesn't create internals states for ProjectState
            RulesProject createdProject = userWorkspace.getProject(repositoryId, createdProjectName);
            if (!userWorkspace.isOpenedOtherProject(createdProject)) {
                createdProject.open();
                createdProjectName = createdProject.getName();
            }
            return createdProject;
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } catch (Exception e) {
            throw new ProjectException("Error creating the project, " + e.getMessage(), e);
        } finally {
            if (projectCreator != null) {
                projectCreator.destroy();
            }
        }
    }

    public String getCreatedProjectName() {
        return createdProjectName;
    }
}
