package org.openl.rules.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/history")
@Tag(name = "History")
public class ProjectHistoryController {

    private final ProjectHistoryService projectHistoryService;

    public ProjectHistoryController(ProjectHistoryService projectHistoryService) {
        this.projectHistoryService = projectHistoryService;
    }

    @Operation(summary = "history.get-project-history.summary", description = "history.get-project-history.desc")
    @GetMapping(value = "/project", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectHistoryItem> getProjectHistory(HttpSession session) {
        var webStudio = WebStudioUtils.getWebStudio(session);
        if (webStudio == null) {
            return Collections.emptyList();
        }
        return projectHistoryService.getProjectHistory(webStudio);
    }

    @Operation(summary = "history.restore.summary", description = "history.restore.desc")
    @PostMapping(value = "/restore", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@Parameter(description = "history.restore.req-body.desc") @RequestBody String versionToRestore,
            HttpSession session) throws Exception {
        var webStudio = WebStudioUtils.getWebStudio(session);
        if (webStudio == null) {
            return;
        }
        projectHistoryService.restore(webStudio, versionToRestore);
    }

    @Operation(summary = "history.delete-all-history.summary", description = "history.delete-all-history.desc")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllHistory() throws IOException {
        projectHistoryService.deleteAllHistory();
    }
}
