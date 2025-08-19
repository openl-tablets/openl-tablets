package org.openl.rules.webstudio.notification.rest.controller;

import java.io.IOException;
import jakarta.servlet.http.HttpSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.notification.service.NotificationService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
@Tag(name = "Notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "notif.get-notif.summary", description = "notif.get-notif.desc")
    @ApiResponse(responseCode = "200", description = "notif.get-notif.200.desc")
    @GetMapping("/public/notification.txt")
    public String getNotification() throws IOException {
        return notificationService.get();
    }

    @Operation(summary = "notif.post-notif.summary", description = "notif.post-notif.desc")
    @PostMapping(value = "/admin/notification.txt", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void postNotification(
            @Parameter(description = "notif.post-notif.req-body.desc") @RequestBody(required = false) String notification) throws IOException {
        notificationService.send(notification);
    }

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
