package org.openl.rules.rest.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
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
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
@Tag(name = "Notification")
public class NotificationController {

    private final Path NOTIFICATION_FILE;

    public NotificationController(@Value("${admin.notification-file}") String notificationFile) {
        this.NOTIFICATION_FILE = Paths.get(notificationFile);
    }

    @Operation(summary = "notif.get-notif.summary", description = "notif.get-notif.desc")
    @ApiResponse(responseCode = "200", description = "notif.get-notif.200.desc")
    @GetMapping("/public/notification.txt")
    public String getNotification() throws IOException {
        if (!Files.exists(NOTIFICATION_FILE)) {
            return null;
        }
        try (Stream<String> lines = Files.lines(NOTIFICATION_FILE)) {
            return lines.collect(Collectors.joining("\r\n"));
        }
    }

    @Operation(summary = "notif.post-notif.summary", description = "notif.post-notif.desc")
    @PostMapping(value = "/admin/notification.txt", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void postNotification(
            @Parameter(description = "notif.post-notif.req-body.desc") @RequestBody(required = false) String notification) throws IOException {
        if (StringUtils.isBlank(notification)) {
            Files.deleteIfExists(NOTIFICATION_FILE);
        } else {
            Files.write(NOTIFICATION_FILE, notification.getBytes(StandardCharsets.UTF_8));
        }
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
