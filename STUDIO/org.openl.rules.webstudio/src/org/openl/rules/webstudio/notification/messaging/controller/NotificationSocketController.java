package org.openl.rules.webstudio.notification.messaging.controller;

import java.io.IOException;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import org.openl.rules.webstudio.notification.event.NotificationEvent;
import org.openl.rules.webstudio.notification.service.NotificationService;
import org.openl.studio.security.AdminPrivilege;

@Controller
public class NotificationSocketController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationSocketController(NotificationService notificationService,
                                        SimpMessagingTemplate messagingTemplate) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @SubscribeMapping("/public/notification.txt")
    public String getNotification() throws IOException {
        return notificationService.get();
    }

    @MessageMapping("/admin/notification.txt")
    @AdminPrivilege
    public void postNotification(@Payload(required = false) String notification) throws IOException {
        notificationService.send(notification);
    }

    @EventListener
    public void onNotificationEvent(NotificationEvent event) {
        var message = event.getMessage();
        messagingTemplate.convertAndSend("/topic/public/notification.txt", message == null ? "" : message);
    }
}
