package org.openl.rules.rest.common.service;

import java.io.IOException;

public interface NotificationService {

    String get() throws IOException;

    void send(String message) throws IOException;

}
