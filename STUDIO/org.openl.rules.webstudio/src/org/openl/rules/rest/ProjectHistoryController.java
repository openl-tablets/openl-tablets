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

@RestController
@RequestMapping("/history")
public class ProjectHistoryController {

    private final ProjectHistoryService projectHistoryService;

    public ProjectHistoryController(ProjectHistoryService projectHistoryService) {
        this.projectHistoryService = projectHistoryService;
    }

    @GetMapping(value = "/project", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectHistoryItem> getProjectHistory(HttpSession session) {
        var webStudio = WebStudioUtils.getWebStudio(session);
        if (webStudio == null) {
            return Collections.emptyList();
        }
        return projectHistoryService.getProjectHistory(webStudio);
    }

    @PostMapping("/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@RequestBody String versionToRestore, HttpSession session) throws Exception {
        var webStudio = WebStudioUtils.getWebStudio(session);
        if (webStudio == null) {
            return;
        }
        projectHistoryService.restore(webStudio, versionToRestore);
    }
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllHistory() throws IOException {
        projectHistoryService.deleteAllHistory();
    }
}
