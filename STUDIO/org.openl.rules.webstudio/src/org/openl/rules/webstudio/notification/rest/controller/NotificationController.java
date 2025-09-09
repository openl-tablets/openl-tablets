package org.openl.rules.webstudio.notification.rest.controller;

import jakarta.servlet.http.HttpSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
@Tag(name = "Notification")
public class NotificationController {

    @Operation(summary = "module.is-module-modified.summary", description = "module.is-module-modified.desc")
    @ApiResponse(responseCode = "200", description = "module.is-module-modified.200.desc")
    @GetMapping("/module/isModified")
    public String isModuleModified(HttpSession httpSession) {
        WebStudio webStudio = WebStudioUtils.getWebStudio(httpSession);
        if (webStudio == null) {
            return null;
        }
        ProjectModel model = webStudio.getModel();
        return Boolean.toString(model.isSourceModified());
    }
}
