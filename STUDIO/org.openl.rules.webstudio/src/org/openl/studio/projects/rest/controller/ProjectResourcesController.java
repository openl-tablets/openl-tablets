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
import org.openl.studio.projects.model.resources.CopyResourceRequest;
import org.openl.studio.projects.model.resources.CreateResourceRequest;
import org.openl.studio.projects.model.resources.MoveResourceRequest;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.studio.projects.model.resources.UpdateResourceRequest;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.resources.ProjectResourcesService;
import org.openl.studio.projects.service.resources.ResourceCriteriaQuery;
import org.openl.studio.projects.service.resources.ResourceViewMode;
import org.openl.studio.projects.validator.resource.ResourceCriteriaQueryValidator;

/**
 * REST controller for project resources (files and folders).
 *
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Resources (BETA)", description = "APIs for managing project resources")
@Validated
public class ProjectResourcesController {

    private static final String PATH_SEPARATOR = "/";

    private final ProjectResourcesService resourcesService;
    private final BeanValidationProvider validationProvider;
    private final ResourceCriteriaQueryValidator queryValidator;

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping("/list/{*path}")
    @Operation(summary = "projects.resources.get.summary", description = "projects.resources.get.desc")
    public List<Resource> getResources(@ProjectId @PathVariable("projectId") RulesProject project,
                                       @PathVariable @Parameter(description = "projects.resources.param.base-path.desc") String path,
                                       @RequestParam(value = "extensions", required = false)
                                       @Parameter(description = "projects.resources.param.extensions.desc")
                                       Set<String> extensions,
                                       @RequestParam(value = "namePattern", required = false)
                                       @Parameter(description = "projects.resources.param.name-pattern.desc")
                                       String namePattern,
                                       @RequestParam(value = "foldersOnly", defaultValue = "false")
                                       @Parameter(description = "projects.resources.param.folders-only.desc")
                                       boolean foldersOnly,
                                       @RequestParam(value = "recursive", defaultValue = "false")
                                       @Parameter(description = "projects.resources.param.recursive.desc")
                                       boolean recursive,
                                       @RequestParam(value = "viewMode", defaultValue = "FLAT")
                                       @Parameter(description = "projects.resources.param.view-mode.desc")
                                       ResourceViewMode viewMode
    ) {
        var basePath = stripLeadingSlash(path);

        var queryBuilder = ResourceCriteriaQuery.builder()
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
    @Operation(summary = "projects.resources.create.summary", description = "projects.resources.create.desc")
    public void createResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.resources.param.base-path.desc") String path,
            @ModelAttribute @Valid CreateResourceRequest request) throws IOException {
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
    @Operation(summary = "projects.resources.download.summary", description = "projects.resources.download.desc")
    public ResponseEntity<byte[]> downloadResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.resources.param.path.desc") String path) throws ProjectException, IOException {
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
    @Operation(summary = "projects.resources.update.summary", description = "projects.resources.update.desc")
    public void updateResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.resources.param.path.desc") String path,
            @ModelAttribute @Valid UpdateResourceRequest request) throws IOException {
        try {
            resourcesService.updateResource(project, stripLeadingSlash(path), request.file().getInputStream());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/copy/{*path}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.resources.copy.summary", description = "projects.resources.copy.desc")
    public void copyResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.resources.param.path.desc") String path,
            @RequestBody @Valid CopyResourceRequest request) {
        try {
            resourcesService.copyResource(project, stripLeadingSlash(path), request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/move/{*path}")
    @Operation(summary = "projects.resources.move.summary", description = "projects.resources.move.desc")
    public void moveResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.resources.param.path.desc") String path,
            @RequestBody @Valid MoveResourceRequest request) {
        try {
            resourcesService.moveResource(project, stripLeadingSlash(path), request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @DeleteMapping("/{*path}")
    @Operation(summary = "projects.resources.delete.summary", description = "projects.resources.delete.desc")
    public void deleteResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @PathVariable @Parameter(description = "projects.resources.param.path.desc") String path) {
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
