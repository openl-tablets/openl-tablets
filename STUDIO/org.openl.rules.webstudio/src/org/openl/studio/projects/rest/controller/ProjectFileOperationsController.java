package org.openl.studio.projects.rest.controller;

import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.files.CopyFileRequest;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.model.files.MoveFileRequest;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.files.FileSearchQuery;
import org.openl.studio.projects.service.files.ProjectFilesService;

/**
 * REST controller for project file operations that involve two paths: copy and move.
 *
 * <p>These operations live outside the {@code /files/{*path}} address space. Both paths
 * travel in the request body, so a command name can never shadow a real file whose path
 * starts with the same segment.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Files (BETA)", description = "APIs for managing project files")
@Validated
public class ProjectFileOperationsController {

    private final ProjectFilesService filesService;

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @PostMapping("/file-copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "projects.files.copy.summary", description = "projects.files.copy.desc")
    public void copyFile(@ProjectId @PathVariable("projectId") RulesProject project,
                         @RequestBody @Valid CopyFileRequest request) {
        try {
            filesService.copyResource(project, request.sourcePath(), request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/file-move")
    @Operation(summary = "projects.files.move.summary", description = "projects.files.move.desc")
    public void moveFile(@ProjectId @PathVariable("projectId") RulesProject project,
                         @RequestBody @Valid MoveFileRequest request) {
        try {
            filesService.moveResource(project, request.sourcePath(), request.destinationPath());
        } finally {
            getWebStudio().reset();
        }
    }

    @PostMapping("/file-search")
    @Operation(summary = "projects.files.search.summary", description = "projects.files.search.desc")
    public List<FsNode> searchFiles(@ProjectId @PathVariable("projectId") RulesProject project,
                                    @RequestBody FileSearchQuery query) {
        return filesService.search(project, query);
    }
}
