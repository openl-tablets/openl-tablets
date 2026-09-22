package org.openl.rules.webstudio.notification.service;

import java.io.IOException;

/**
 * The notification OpenL Studio shows to every user.
 */
public interface NotificationService {

    /**
     * Returns the current notification, or {@code null} when there is none.
     */
    String get() throws IOException;

    /**
     * Replaces the notification and tells every connected user about it.
     *
     * <p>Control characters other than line breaks and tabs are dropped from the message. A blank message
     * removes the notification.
     */
    void send(String message) throws IOException;

}
