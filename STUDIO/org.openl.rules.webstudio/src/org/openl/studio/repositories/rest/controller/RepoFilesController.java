package org.openl.studio.repositories.rest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.rest.controller.AbstractFilesController;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.files.RepoFileRootFactory;
import org.openl.studio.projects.validator.file.FileCriteriaQueryValidator;
import org.openl.studio.repositories.rest.resolver.DesignRepository;

/**
 * REST controller for repository files and folders.
 *
 * <p>Mounts the files API on a design repository: {@code /repos/{repo-name}/files/{*path}}. Unlike
 * the project mount, there is no workspace copy — operations apply directly to the repository on the
 * selected branch. The {@code branch} parameter selects the branch to operate on.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/repos/{repo-name}/files", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Repositories: Files (BETA)", description = "APIs for managing repository files")
@Validated
public class RepoFilesController extends AbstractFilesController {

    private final RepoFileRootFactory fileRootFactory;

    public RepoFilesController(ProjectFilesService filesService,
                               RepoFileRootFactory fileRootFactory,
                               BeanValidationProvider validationProvider,
                               FileCriteriaQueryValidator queryValidator) {
        super(filesService, validationProvider, queryValidator);
        this.fileRootFactory = fileRootFactory;
    }

    @PostMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create.summary", description = "projects.files.create.desc")
    public void createResource(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam("file") @Parameter(description = "projects.files.param.file.desc") List<MultipartFile> files,
            @RequestParam(value = "createFolders", defaultValue = "false")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "conflictPolicy", defaultValue = "FAIL")
            @Parameter(description = "projects.files.param.conflict-policy.desc") ConflictPolicy conflictPolicy,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) throws IOException {
        handleCreate(fileRootFactory.of(repository, branch), path, files, createFolders, conflictPolicy);
    }

    @PostMapping(value = "/{*path}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create.summary", description = "projects.files.create.desc")
    public void createResourceRaw(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "createFolders", defaultValue = "false")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) {
        handleCreateRaw(fileRootFactory.of(repository, branch), path, createFolders, content);
    }

    @PostMapping(value = "/{*path}", consumes = "application/zip")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.upload-archive.summary", description = "projects.files.upload-archive.desc")
    public void uploadArchive(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "createFolders", defaultValue = "true")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "conflictPolicy", defaultValue = "FAIL")
            @Parameter(description = "projects.files.param.conflict-policy.desc") ConflictPolicy conflictPolicy,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) throws IOException {
        handleUploadArchive(fileRootFactory.of(repository, branch), path, createFolders, conflictPolicy, content);
    }

    @GetMapping(value = "/{*path}", produces = MediaType.ALL_VALUE)
    @Operation(summary = "projects.files.get.summary", description = "projects.files.get.desc")
    public ResponseEntity<?> getFile(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "view", required = false)
            @Parameter(description = "projects.files.param.view.desc") String view,
            @RequestParam(value = "download", required = false)
            @Parameter(description = "projects.files.param.download.desc") String download,
            @RequestParam(value = "extensions", required = false)
            @Parameter(description = "projects.files.param.extensions.desc") Set<String> extensions,
            @RequestParam(value = "namePattern", required = false)
            @Parameter(description = "projects.files.param.name-pattern.desc") String namePattern,
            @RequestParam(value = "foldersOnly", defaultValue = "false")
            @Parameter(description = "projects.files.param.folders-only.desc") boolean foldersOnly,
            @RequestParam(value = "recursive", defaultValue = "false")
            @Parameter(description = "projects.files.param.recursive.desc") boolean recursive,
            @RequestParam(value = "viewMode", defaultValue = "FLAT")
            @Parameter(description = "projects.files.param.view-mode.desc") FileViewMode viewMode,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            @RequestParam(value = "version", required = false)
            @Parameter(description = "projects.files.param.version.desc") String version
    ) throws ProjectException, IOException {
        return handleGetFile(fileRootFactory.of(repository, branch), path, view, download, extensions, namePattern,
                foldersOnly, recursive, viewMode, version);
    }

    @PutMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "projects.files.update.summary", description = "projects.files.update.desc")
    public void updateResource(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam("file") @Parameter(description = "projects.files.param.file.desc") MultipartFile file,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) throws IOException {
        handleUpdate(fileRootFactory.of(repository, branch), path, file);
    }

    @PutMapping(value = "/{*path}")
    @Operation(summary = "projects.files.update.summary", description = "projects.files.update.desc")
    public void updateResourceRaw(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) {
        handleUpdateRaw(fileRootFactory.of(repository, branch), path, content);
    }

    @PutMapping(value = "/{*path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create-folder.summary", description = "projects.files.create-folder.desc")
    public void createFolder(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "createFolders", defaultValue = "true")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) {
        handleCreateFolder(fileRootFactory.of(repository, branch), path, createFolders);
    }

    @DeleteMapping("/{*path}")
    @Operation(summary = "projects.files.delete.summary", description = "projects.files.delete.desc")
    public void deleteResource(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) {
        handleDelete(fileRootFactory.of(repository, branch), path);
    }
}
