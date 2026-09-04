package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.studio.projects.model.history.ProjectHistoryItem;
import org.openl.studio.projects.model.history.RestoreProjectHistoryRequest;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.history.ProjectHistoryService;
import org.openl.studio.security.AdminPrivilege;
import org.openl.util.StringUtils;

@RestController
@RequiredArgsConstructor
@Tag(name = "History")
public class ProjectHistoryController {

    private final ProjectHistoryService projectHistoryService;

    @Operation(summary = "history.get-local-history.summary", description = "history.get-local-history.desc")
    @GetMapping(value = "/projects/{projectId}/local-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectHistoryItem> getLocalHistory(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "history.get-local-history.param.module.desc")
            @RequestParam(value = "module", required = false) @Nullable String module) {
        return projectHistoryService.getLocalHistory(project, StringUtils.trimToNull(module));
    }

    @Operation(summary = "history.restore.summary", description = "history.restore.desc")
    @PostMapping(value = "/projects/{projectId}/local-history/restore", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void restore(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @Parameter(description = "history.get-local-history.param.module.desc")
            @RequestParam(value = "module", required = false) @Nullable String module,
            @Parameter(description = "history.restore.req-body.desc")
            @Valid @RequestBody RestoreProjectHistoryRequest request,
            HttpSession session) throws Exception {
        var webStudio = WebStudioUtils.getWebStudio(session);
        projectHistoryService.restore(project,
                StringUtils.trimToNull(module),
                request.version().strip(),
                webStudio);
    }

    @Operation(summary = "history.delete-project-history.summary", description = "history.delete-project-history.desc")
    @DeleteMapping("/projects/{projectId}/local-history")
    public void deleteProjectHistory(@ProjectId @PathVariable("projectId") RulesProject project) throws IOException {
        projectHistoryService.deleteProjectHistory(project);
    }

    @AdminPrivilege
    @Operation(summary = "history.delete-all-history.summary", description = "history.delete-all-history.desc")
    @DeleteMapping("/admin/local-history")
    public void deleteAllHistory() throws IOException {
        projectHistoryService.deleteAllHistory();
    }
}
