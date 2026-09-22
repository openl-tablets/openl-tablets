package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
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
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.validator.file.FileCriteriaQueryValidator;
import org.openl.util.StringUtils;

/**
 * REST controller for project files and folders.
 *
 * <p>Mounts the files API on a project's working copy: {@code /projects/{projectId}/files/{*path}}.
 * Writes stage in the working copy. The {@code branch} parameter asserts the project is on the
 * expected branch; it does not switch branches.
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/files", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Files (BETA)", description = ProjectFilesController.TAG_DESCRIPTION)
@Validated
public class ProjectFilesController extends AbstractFilesController {

    /**
     * Shared by every controller of the same OpenAPI tag, which merges by the tag name.
     */
    static final String TAG_DESCRIPTION = """
            APIs for managing project files. A modifying operation locks \
            the project for editing; the lock is released when the project is saved or closed. A closed \
            project is modified directly in the design repository and is not left locked.""";

    private final ProjectFileRootFactory fileRootFactory;

    public ProjectFilesController(ProjectFilesService filesService,
                                  ProjectFileRootFactory fileRootFactory,
                                  BeanValidationProvider validationProvider,
                                  FileCriteriaQueryValidator queryValidator) {
        super(filesService, validationProvider, queryValidator);
        this.fileRootFactory = fileRootFactory;
    }

    @Lookup
    public WebStudio getWebStudio() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    @Override
    protected void postWrite() {
        getWebStudio().reset();
    }

    @PostMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create.summary", description = "projects.files.create.desc")
    public void createResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam("file") @Parameter(description = "projects.files.param.file.desc") List<MultipartFile> files,
            @RequestParam(value = "createFolders", defaultValue = "false")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "conflictPolicy", defaultValue = "FAIL")
            @Parameter(description = "projects.files.param.conflict-policy.desc") ConflictPolicy conflictPolicy,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) throws IOException {
        BranchGuard.requireBranch(project, branch);
        handleCreate(fileRootFactory.of(project), path, files, createFolders, conflictPolicy);
    }

    @PostMapping("/{*path}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create.summary", description = "projects.files.create.desc")
    public void createResourceRaw(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "createFolders", defaultValue = "false")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) {
        BranchGuard.requireBranch(project, branch);
        handleCreateRaw(fileRootFactory.of(project), path, createFolders, content);
    }

    @PostMapping(value = "/{*path}", consumes = "application/zip")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.upload-archive.summary", description = "projects.files.upload-archive.desc")
    public void uploadArchive(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "createFolders", defaultValue = "true")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "conflictPolicy", defaultValue = "FAIL")
            @Parameter(description = "projects.files.param.conflict-policy.desc") ConflictPolicy conflictPolicy,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) throws IOException {
        BranchGuard.requireBranch(project, branch);
        handleUploadArchive(fileRootFactory.of(project), path, createFolders, conflictPolicy, content);
    }

    @GetMapping(value = "/{*path}", produces = MediaType.ALL_VALUE)
    @Operation(summary = "projects.files.get.summary", description = "projects.files.get.desc")
    public ResponseEntity<?> getFile(
            @ProjectId @PathVariable("projectId") RulesProject project,
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
            @Parameter(description = "projects.files.param.version.desc") String version,
            @RequestParam(value = "zone", required = false)
            @Parameter(description = "projects.files.param.zone.desc") String zone,
            HttpServletResponse response
    ) throws ProjectException, IOException {
        BranchGuard.requireBranch(project, branch);
        var rootArchiveName = isRootDownload(path, download)
                ? getProjectArchiveName(project, version, zoneOf(zone))
                : null;
        return handleGetFile(fileRootFactory.of(project), path, view, download, extensions, namePattern,
                foldersOnly, recursive, viewMode, version, response, rootArchiveName);
    }

    private static boolean isRootDownload(String path, String download) {
        return download != null && (path == null || path.isEmpty() || "/".equals(path));
    }

    /**
     * What the downloaded archive is called: the project's business name and the revision it holds.
     *
     * <p>A revision asked for is named by that revision rather than by where the project stands now. The
     * two part as soon as anything is saved after it, and an archive of an older revision named for the
     * newest reads as holding what it does not.
     */
    static String getProjectArchiveName(RulesProject project, @Nullable String version, ZoneId zone)
            throws IOException {
        if (StringUtils.isBlank(version)) {
            project.refresh();
            return getProjectArchiveName(project.getBusinessName(), project.getFileData(), zone);
        }
        return getProjectArchiveName(project.getBusinessName(), revisionOf(project, version), zone);
    }

    /**
     * What the design repository records about the project at that revision: who wrote it and when.
     *
     * <p>Asked of the repository rather than of a project read at that revision. A project reads its own
     * file data by first asking whether the revision is the latest, and a repository that keeps versions
     * answers that by walking its history until it finds the project — work the name has no use for.
     *
     * <p>A revision nothing is recorded for is answered with nothing: the archive then carries the
     * project's name alone, and the download itself refuses the unknown revision.
     */
    private static @Nullable FileData revisionOf(RulesProject project, String version) throws IOException {
        var design = project.getDesignRepository();
        return design == null ? null : design.checkHistory(project.getDesignFolderName(), version);
    }

    /** The zone the caller reads times in, or the one this machine stands in when they name none. */
    private static ZoneId zoneOf(@Nullable String zone) {
        if (StringUtils.isBlank(zone)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(zone.trim());
        } catch (DateTimeException unknown) {
            return ZoneId.systemDefault();
        }
    }

    static String getProjectArchiveName(String businessName, @Nullable FileData fileData, ZoneId zone) {
        // A repository can report a project as existing with nothing but its name filled in — its folder
        // has gone — and there is no moment to name such an archive after.
        if (fileData == null || fileData.getModifiedAt() == null) {
            return businessName + ".zip";
        }
        return "%s-%s.zip".formatted(businessName, RepositoryUtils.buildProjectVersion(fileData, zone));
    }

    @PutMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "projects.files.update.summary", description = "projects.files.update.desc")
    public void updateResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam("file") @Parameter(description = "projects.files.param.file.desc") MultipartFile file,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) throws IOException {
        BranchGuard.requireBranch(project, branch);
        handleUpdate(fileRootFactory.of(project), path, file);
    }

    @PutMapping("/{*path}")
    @Operation(summary = "projects.files.update.summary", description = "projects.files.update.desc")
    public void updateResourceRaw(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) {
        BranchGuard.requireBranch(project, branch);
        handleUpdateRaw(fileRootFactory.of(project), path, content);
    }

    @PutMapping(value = "/{*path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create-folder.summary", description = "projects.files.create-folder.desc")
    public void createFolder(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "createFolders", defaultValue = "true")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) {
        BranchGuard.requireBranch(project, branch);
        handleCreateFolder(fileRootFactory.of(project), path, createFolders);
    }

    @DeleteMapping("/{*path}")
    @Operation(summary = "projects.files.delete.summary", description = "projects.files.delete.desc")
    public void deleteResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) {
        BranchGuard.requireBranch(project, branch);
        handleDelete(fileRootFactory.of(project), path);
    }
}
