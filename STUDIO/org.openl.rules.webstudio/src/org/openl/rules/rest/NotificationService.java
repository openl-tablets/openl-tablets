package org.openl.rules.rest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class NotificationService {

    @Value("${admin.notification-file}")
    private File NOTIFICATION_FILE;

    @GET
    @Path("/public/notification.txt")
    public String getNotification() throws IOException {
        if (!Files.exists(NOTIFICATION_FILE.toPath())) {
            return null;
        }
        try (Stream<String> lines = Files.lines(NOTIFICATION_FILE.toPath())) {
            return lines.collect(Collectors.joining("\r\n"));
        }
    }

    @POST
    @Path("/admin/notification.txt")
    public void postNotification(String notification) throws IOException {
        if (StringUtils.isBlank(notification)) {
            Files.deleteIfExists(NOTIFICATION_FILE.toPath());
        } else {
            Files.write(NOTIFICATION_FILE.toPath(), notification.getBytes(StandardCharsets.UTF_8));
        }
    }
}
