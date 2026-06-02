package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.model.files.CopyFileRequest;
import org.openl.studio.projects.model.files.CreateFileRequest;
import org.openl.studio.projects.model.files.MoveFileRequest;
import org.openl.studio.projects.model.files.ProjectFileLookupResponse;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.model.files.UpdateFileRequest;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.files.ProjectFileLookupService;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.files.FileCriteriaQuery;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.validator.file.FileCriteriaQueryValidator;

/**
 * REST controller for project resources (files and folders).
 *
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/files", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Files (BETA)", description = "APIs for managing project files")
@Validated
public class ProjectFilesController {

    private static final String PATH_SEPARATOR = "/";

    private final ProjectFilesService resourcesService;
    private final ProjectFileLookupService fileLookupService;
    private final BeanValidationProvider validationProvider;
    private final FileCriteriaQueryValidator queryValidator;

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping
    @Operation(summary = "projects.files.lookup.summary", description = "projects.files.lookup.desc")
    public ProjectFileLookupResponse lookupFile(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam("path")
            @Parameter(description = "projects.files.lookup.param.path.desc")
            String path,
            @RequestParam(value = "searchParents", defaultValue = "false")
            @Parameter(description = "projects.files.lookup.param.search-parents.desc")
            boolean searchParents,
            @RequestParam(value = "includeContent", defaultValue = "false")
            @Parameter(description = "projects.files.lookup.param.include-content.desc")
            boolean includeContent
    ) throws IOException {
        return fileLookupService.lookup(project, path, searchParents, includeContent);
    }

    @GetMapping("/list/{*path}")
    @Operation(summary = "projects.files.get.summary", description = "projects.files.get.desc")
    public List<FsNode> getResources(@ProjectId @PathVariable("projectId") RulesProject project,
                                       @PathVariable @Parameter(description = "projects.files.param.base-path.desc") String path,
                                       @RequestParam(value = "extensions", required = false)
                                       @Parameter(description = "projects.files.param.extensions.desc")
                                       Set<String> extensions,
                                       @RequestParam(value = "namePattern", required = false)
                                       @Parameter(description = "projects.files.param.name-pattern.desc")
                                       String namePattern,
                                       @RequestParam(value = "foldersOnly", defaultValue = "false")
                                       @Parameter(description = "projects.files.param.folders-only.desc")
                                       boolean foldersOnly,
                                       @RequestParam(value = "recursive", defaultValue = "false")
                                       @Parameter(description = "projects.files.param.recursive.desc")
                                       boolean recursive,
                                       @RequestParam(value = "viewMode", defaultValue = "FLAT")
                                       @Parameter(description = "projects.files.param.view-mode.desc")
                                       FileViewMode viewMode
    ) {
        var basePath = stripLeadingSlash(path);

        var queryBuilder = FileCriteriaQuery.builder()
                .basePath(basePath.isEmpty() ? null : basePath)
                .namePattern(namePattern)
                .foldersOnly(foldersOnly);
        if (extensions != null) {
            queryBuilder.extensions(extensions);
        }
        var query = queryBuilder.build();
        validationProvider.validate(query, queryValidator);

        return resourcesService.getResources(project, query, recursive, viewMode);
    }

    @PostMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.create.summary", description = "projects.files.create.desc")
    public void createResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.base-path.desc") String path,
            @ModelAttribute @Valid CreateFileRequest request) throws IOException {
        var basePath = stripLeadingSlash(path);
        var fullPath = basePath.isEmpty() ? request.relativePath() : String.join(PATH_SEPARATOR, basePath, request.relativePath());
        try {
            resourcesService.createResource(project, fullPath, request.file().getInputStream(),
                    request.createFolders());
        } finally {
            getWebStudio().reset();
        }
    }

    @GetMapping(value = "/{*path}", produces = MediaType.ALL_VALUE)
    @Operation(summary = "projects.files.download.summary", description = "projects.files.download.desc")
    public ResponseEntity<byte[]> downloadResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path) throws ProjectException, IOException {
        var resource = resourcesService.getResource(project, stripLeadingSlash(path));
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
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @ModelAttribute @Valid UpdateFileRequest request) throws IOException {
        try {
            resourcesService.updateResource(project, stripLeadingSlash(path), request.file().getInputStream());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/copy/{*path}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.copy.summary", description = "projects.files.copy.desc")
    public void copyResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestBody @Valid CopyFileRequest request) {
        try {
            resourcesService.copyResource(project, stripLeadingSlash(path), request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/move/{*path}")
    @Operation(summary = "projects.files.move.summary", description = "projects.files.move.desc")
    public void moveResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path,
            @RequestBody @Valid MoveFileRequest request) {
        try {
            resourcesService.moveResource(project, stripLeadingSlash(path), request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @DeleteMapping("/{*path}")
    @Operation(summary = "projects.files.delete.summary", description = "projects.files.delete.desc")
    public void deleteResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.files.param.path.desc") String path) {
        try {
            resourcesService.deleteResource(project, stripLeadingSlash(path));
        } finally {
            getWebStudio().reset();
        }
    }

    /**
     * Strips the leading slash from the path captured by {@code {*path}}.
     * Spring's catch-all path variable includes a leading '/' (e.g., "/folder/file.xlsx").
     */
    private static String stripLeadingSlash(String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
