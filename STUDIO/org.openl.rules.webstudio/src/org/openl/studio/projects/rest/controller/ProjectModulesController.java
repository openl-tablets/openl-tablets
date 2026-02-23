package org.openl.studio.projects.rest.controller;

import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.modules.CopyModuleRequest;
import org.openl.studio.projects.model.modules.CopyModuleResponse;
import org.openl.studio.projects.model.modules.EditModuleRequest;
import org.openl.studio.projects.model.modules.ModuleView;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.ProjectModulesService;

/**
 * REST controller for project module operations.
 */
@Validated
@RestController
@RequestMapping(value = "/projects/{projectId}/modules", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Modules (BETA)", description = "Experimental projects modules API")
public class ProjectModulesController {

    private final ProjectModulesService modulesService;

    public ProjectModulesController(ProjectModulesService modulesService) {
        this.modulesService = modulesService;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @Operation(summary = "projects.modules.list.summary", description = "projects.modules.list.desc")
    @ApiResponse(responseCode = "200", description = "projects.modules.list.200.desc")
    @GetMapping
    public List<ModuleView> getModules(@ProjectId @PathVariable("projectId") RulesProject project) {
        return modulesService.getModules(project);
    }

    @Operation(summary = "projects.modules.copy.summary", description = "projects.modules.copy.desc")
    @ApiResponse(responseCode = "201", description = "projects.modules.copy.201.desc")
    @PostMapping("/{moduleName}/copy")
    public ResponseEntity<CopyModuleResponse> copyModule(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.modules.copy.param.moduleName.desc")
            @PathVariable("moduleName") String moduleName,
            @RequestBody @Valid CopyModuleRequest request,
            @Parameter(description = "projects.modules.copy.param.force.desc")
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        try {
            var response = modulesService.copyModule(project, moduleName, request, force);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "projects.modules.add.summary", description = "projects.modules.add.desc")
    @ApiResponse(responseCode = "201", description = "projects.modules.add.201.desc")
    @PostMapping
    public ResponseEntity<ModuleView> addModule(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestBody @Valid EditModuleRequest request) {
        try {
            var response = modulesService.addModule(project, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "projects.modules.edit.summary", description = "projects.modules.edit.desc")
    @ApiResponse(responseCode = "200", description = "projects.modules.edit.200.desc")
    @PutMapping("/{moduleName}")
    public ModuleView editModule(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.modules.edit.param.moduleName.desc")
            @PathVariable("moduleName") String moduleName,
            @RequestBody @Valid EditModuleRequest request) {
        try {
            return modulesService.editModule(project, moduleName, request);
        } finally {
            getWebStudio().reset();
        }
    }

    @Operation(summary = "projects.modules.remove.summary", description = "projects.modules.remove.desc")
    @ApiResponse(responseCode = "204", description = "projects.modules.remove.204.desc")
    @DeleteMapping("/{moduleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeModule(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "projects.modules.remove.param.moduleName.desc")
            @PathVariable("moduleName") String moduleName,
            @Parameter(description = "projects.modules.remove.param.keepFile.desc")
            @RequestParam(value = "keepFile", defaultValue = "false") boolean keepFile) {
        try {
            modulesService.removeModule(project, moduleName, keepFile);
        } finally {
            getWebStudio().reset();
        }
    }
}
