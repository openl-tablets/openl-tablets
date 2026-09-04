package org.openl.rules.webstudio.web.repository.upload;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import lombok.Getter;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclRole;
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
    private final Repository repository;
    private final String modelsPath;
    private final String algorithmsPath;
    private final String modelsModuleName;
    private final String algorithmsModuleName;
    private final Consumer<RulesProject> finalizeProject;
    private final Runnable awaitProjectVisibility;
    @Getter
    private String createdProjectName;
    private final Map<String, String> tags;

    public ProjectUploader(Repository repository,
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
                           String algorithmsModuleName,
                           Map<String, String> tags,
                           Consumer<RulesProject> finalizeProject,
                           Runnable awaitProjectVisibility) {
        this.repository = repository;
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
        this.tags = tags;
        this.finalizeProject = finalizeProject;
        this.awaitProjectVisibility = awaitProjectVisibility;
    }

    public RulesProject uploadProject() throws ProjectException {
        AProjectCreator projectCreator = null;
        if (uploadedFiles.isEmpty()) {
            throw new ProjectException("Cannot create project from the given file.");
        }
        try {
            // Get the last file
            var file = uploadedFiles.getLast();
            var fileName = file.getName();
            if (FileTypeHelper.isPossibleOpenAPIFile(fileName)) {
                projectCreator = new OpenAPIProjectCreator(repository,
                        file,
                        projectName,
                        projectFolder,
                        userWorkspace,
                        comment,
                        modelsPath,
                        algorithmsPath,
                        modelsModuleName,
                        algorithmsModuleName,
                        tags);
            } else if (FileTypeHelper.isZipFile(fileName)) {
                // Create project creator for the single zip file
                projectCreator = new ZipFileProjectCreator(repository,
                        fileName,
                        file.getInput(),
                        projectName,
                        projectFolder,
                        userWorkspace,
                        comment,
                        zipFilter,
                        zipCharsetDetector,
                        tags);
            } else {
                projectCreator = new ExcelFilesProjectCreator(repository,
                        projectName,
                        projectFolder,
                        userWorkspace,
                        comment,
                        zipFilter,
                        tags,
                        uploadedFiles.toArray(new ProjectFile[0]));
            }
            var rulesProject = projectCreator.createRulesProject();
            if (!designRepositoryAclService.createAcl(rulesProject,
                    List.of(AclRole.CONTRIBUTOR.getCumulativePermission()),
                    true)) {
                throw new ProjectException("Granting permissions to a new project '%s' is failed.".formatted(
                        ProjectArtifactUtils.extractResourceName(rulesProject)));
            }
            createdProjectName = projectCreator.getCreatedProjectName();
            finalizeProject.accept(rulesProject);
            awaitProjectVisibility.run();
            // The controller applies the requested workspace status after the durable write. Resolving the project
            // here by repository and name alone loses its branch and can turn a successful create into an error.
            return rulesProject;
        } catch (IOException e) {
            throw new ProjectException(e.getMessage(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ProjectException("Error creating the project, " + e.getMessage(), e);
        } finally {
            if (projectCreator != null) {
                projectCreator.destroy();
            }
        }
    }
}
