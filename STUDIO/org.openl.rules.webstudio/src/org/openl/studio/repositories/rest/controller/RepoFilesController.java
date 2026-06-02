package org.openl.studio.repositories.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.projects.service.files.FileCriteriaQuery;
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
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/repos/{repo-name}/files", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Repositories: Files (BETA)", description = "APIs for managing repository files")
@Validated
public class RepoFilesController {

    private final ProjectFilesService filesService;
    private final RepoFileRootFactory fileRootFactory;
    private final BeanValidationProvider validationProvider;
    private final FileCriteriaQueryValidator queryValidator;

    @PostMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create.summary", description = "projects.files.create.desc")
    public void createResource(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam("file") @Parameter(description = "projects.files.param.file.desc") MultipartFile file,
            @RequestParam(value = "createFolders", defaultValue = "false")
            @Parameter(description = "projects.files.param.create-folders.desc") boolean createFolders,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) throws IOException {
        filesService.createResource(fileRootFactory.of(repository, branch), stripLeadingSlash(path), file.getInputStream(), createFolders);
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
        filesService.createResource(fileRootFactory.of(repository, branch), stripLeadingSlash(path), content, createFolders);
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
        if (!isFolderPath(path)) {
            throw new BadRequestException("file.path.requires.content.message");
        }
        filesService.uploadArchive(fileRootFactory.of(repository, branch), stripSlashes(path), content, createFolders, conflictPolicy);
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
        var root = fileRootFactory.of(repository, branch);
        if (isFolderPath(path)) {
            var basePath = stripSlashes(path);
            if (download != null) {
                String zipName = (basePath.isEmpty() ? "files" : basePath.substring(basePath.lastIndexOf('/') + 1)) + ".zip";
                StreamingResponseBody body = out -> filesService.writeFolderAsZip(root, basePath, out, version);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/zip"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(zipName))
                        .body(body);
            }
            var queryBuilder = FileCriteriaQuery.builder()
                    .basePath(basePath.isEmpty() ? null : basePath)
                    .namePattern(namePattern)
                    .foldersOnly(foldersOnly);
            if (extensions != null) {
                queryBuilder.extensions(extensions);
            }
            var query = queryBuilder.build();
            validationProvider.validate(query, queryValidator);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(filesService.getResources(root, query, recursive, viewMode, version));
        }
        var filePath = stripLeadingSlash(path);
        if ("meta".equalsIgnoreCase(view)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(filesService.getNode(root, filePath, version));
        }
        var resource = filesService.getResource(root, filePath, version);
        var output = new ByteArrayOutputStream();
        try (var stream = resource.getContent()) {
            stream.transferTo(output);
        }
        String fileName = resource.getName();
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(fileName))
                .body(output.toByteArray());
    }

    @PutMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "projects.files.update.summary", description = "projects.files.update.desc")
    public void updateResource(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam("file") @Parameter(description = "projects.files.param.file.desc") MultipartFile file,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) throws IOException {
        filesService.updateResource(fileRootFactory.of(repository, branch), stripLeadingSlash(path), file.getInputStream());
    }

    @PutMapping(value = "/{*path}")
    @Operation(summary = "projects.files.update.summary", description = "projects.files.update.desc")
    public void updateResourceRaw(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch,
            InputStream content) {
        filesService.updateResource(fileRootFactory.of(repository, branch), stripLeadingSlash(path), content);
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
        if (!isFolderPath(path)) {
            throw new BadRequestException("file.path.requires.content.message");
        }
        filesService.createFolder(fileRootFactory.of(repository, branch), stripSlashes(path), createFolders);
    }

    @DeleteMapping("/{*path}")
    @Operation(summary = "projects.files.delete.summary", description = "projects.files.delete.desc")
    public void deleteResource(
            @DesignRepository("repo-name") Repository repository,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestParam(value = "branch", required = false)
            @Parameter(description = "projects.files.param.branch.desc") String branch) {
        filesService.deleteResource(fileRootFactory.of(repository, branch), stripLeadingSlash(path));
    }

    /**
     * Determines whether the captured path addresses a folder. A trailing slash, or the
     * empty/root path, denotes a folder; any other path denotes a file.
     */
    private static boolean isFolderPath(String path) {
        return path == null || path.isEmpty() || path.equals("/") || path.endsWith("/");
    }

    /**
     * Strips leading and trailing slashes from the path captured by {@code {*path}}.
     */
    private static String stripSlashes(String path) {
        var result = stripLeadingSlash(path);
        while (result != null && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Strips the leading slash from the path captured by {@code {*path}}.
     */
    private static String stripLeadingSlash(String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
