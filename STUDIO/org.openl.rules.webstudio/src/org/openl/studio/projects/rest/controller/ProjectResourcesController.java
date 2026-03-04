package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.openl.studio.projects.model.resources.CopyResourceRequest;
import org.openl.studio.projects.model.resources.CreateResourceRequest;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.studio.projects.model.resources.UpdateResourceRequest;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.resources.ProjectResourcesService;
import org.openl.studio.projects.service.resources.ResourceCriteriaQuery;
import org.openl.studio.projects.service.resources.ResourceViewMode;

/**
 * REST controller for project resources (files and folders).
 *
 */
@RestController
@RequestMapping(value = "/projects/{projectId}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Resources (BETA)", description = "APIs for managing project resources")
@Validated
public class ProjectResourcesController {

    private final ProjectResourcesService resourcesService;

    public ProjectResourcesController(ProjectResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping
    @Operation(summary = "projects.resources.get.summary", description = "projects.resources.get.desc")
    public List<Resource> getResources(@ProjectId @PathVariable("projectId") RulesProject project,
                                       @RequestParam(value = "basePath", required = false)
                                       @Parameter(description = "projects.resources.param.base-path.desc")
                                       String basePath,
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

        var query = ResourceCriteriaQuery.builder()
                .basePath(basePath)
                .extensions(extensions)
                .namePattern(namePattern)
                .foldersOnly(foldersOnly)
                .build();

        return resourcesService.getResources(project, query, recursive, viewMode);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.resources.create.summary", description = "projects.resources.create.desc")
    public void createResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @ModelAttribute @Valid CreateResourceRequest request) throws IOException {
        try {
            resourcesService.createResource(project, request.path(), request.file().getInputStream(),
                    request.createFolders());
        } finally {
            getWebStudio().reset();
        }
    }

    @GetMapping(value = "/{resourceId}", produces = MediaType.ALL_VALUE)
    @Operation(summary = "projects.resources.download.summary", description = "projects.resources.download.desc")
    public ResponseEntity<byte[]> downloadResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.resources.param.resource-id.desc")
            @PathVariable("resourceId") String resourceId) throws ProjectException, IOException {
        var resource = resourcesService.getResource(project, resourceId);
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

    @PutMapping(value = "/{resourceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "projects.resources.update.summary", description = "projects.resources.update.desc")
    public void updateResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.resources.param.resource-id.desc")
            @PathVariable("resourceId") String resourceId,
            @ModelAttribute @Valid UpdateResourceRequest request) throws IOException {
        try {
            resourcesService.updateResource(project, resourceId, request.file().getInputStream());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/{resourceId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.resources.copy.summary", description = "projects.resources.copy.desc")
    public void copyResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.resources.param.resource-id.desc")
            @PathVariable("resourceId") String resourceId,
            @RequestBody @Valid CopyResourceRequest request) {
        try {
            resourcesService.copyResource(project, resourceId, request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @DeleteMapping("/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "projects.resources.delete.summary", description = "projects.resources.delete.desc")
    public void deleteResource(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.resources.param.resource-id.desc")
            @PathVariable("resourceId") String resourceId) {
        try {
            resourcesService.deleteResource(project, resourceId);
        } finally {
            getWebStudio().reset();
        }
    }
}
