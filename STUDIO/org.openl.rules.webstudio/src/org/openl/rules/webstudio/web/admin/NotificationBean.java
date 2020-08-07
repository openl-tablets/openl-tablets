package org.openl.rules.webstudio.web.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.rules.webstudio.web.Props;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class NotificationBean {

    private final String NOTIFICATION_FILE_NAME = "notification.txt";
    private final String NOTIFICATION_FILE_NAME_PATH = Props
        .text("openl.home.shared") + File.separator + NOTIFICATION_FILE_NAME;

    private String value = "";

    public String getValue() throws IOException {
        File notificationFile = new File(NOTIFICATION_FILE_NAME_PATH);
        if (!notificationFile.exists()) {
            return "";
        }
        Stream<String> lines = Files.lines(Paths.get(notificationFile.getAbsolutePath()));
        return lines.collect(Collectors.joining(" "));
    }

    public void setValue(String notification) {
        this.value = notification;
    }

    public void post() throws IOException {
        BufferedWriter writer = null;
        try {
            File notificationFile = new File(NOTIFICATION_FILE_NAME_PATH);
            if (!notificationFile.exists()) {
                notificationFile.createNewFile();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(notificationFile.getAbsolutePath()),
                StandardCharsets.UTF_8));
            writer.write(value);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void remove() {
        value = "";
        File notificationFile = new File(NOTIFICATION_FILE_NAME_PATH);
        if (notificationFile.exists()) {
            notificationFile.delete();
        }
    }
}
