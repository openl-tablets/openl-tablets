package org.openl.rules.rest.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
public class NotificationController {

    private final Path NOTIFICATION_FILE;

    public NotificationController(@Value("${admin.notification-file}") String notificationFile) {
        this.NOTIFICATION_FILE = Paths.get(notificationFile);
    }

    @GetMapping("/public/notification.txt")
    public String getNotification() throws IOException {
        if (!Files.exists(NOTIFICATION_FILE)) {
            return null;
        }
        try (Stream<String> lines = Files.lines(NOTIFICATION_FILE)) {
            return lines.collect(Collectors.joining("\r\n"));
        }
    }

    @PostMapping(value = "/admin/notification.txt", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void postNotification(String notification) throws IOException {
        if (StringUtils.isBlank(notification)) {
            Files.deleteIfExists(NOTIFICATION_FILE);
        } else {
            Files.write(NOTIFICATION_FILE, notification.getBytes(StandardCharsets.UTF_8));
        }
    }

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
