package org.openl.rules.rest.common;

import java.io.IOException;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import org.openl.rules.rest.common.service.NotificationService;
import org.openl.rules.security.AdminPrivilege;

@Controller
public class NotificationSocketController {

    private final NotificationService notificationService;

    public NotificationSocketController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @SubscribeMapping("/public/notification.txt")
    public String getNotification() throws IOException {
        return notificationService.get();
    }

    @MessageMapping("/admin/notification.txt")
    @AdminPrivilege
    public void postNotification(String notification) throws IOException {
        notificationService.send(notification);
    }
}
