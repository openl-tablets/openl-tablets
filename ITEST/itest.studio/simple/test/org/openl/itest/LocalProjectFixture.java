package org.openl.itest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipInputStream;

final class LocalProjectFixture {

    private LocalProjectFixture() {
    }

    static void seed(Path userDir, String projectName, Path zip) throws IOException {
        var projectDir = userDir.resolve(projectName).normalize();
        try (var stream = new ZipInputStream(Files.newInputStream(zip))) {
            for (var entry = stream.getNextEntry(); entry != null; entry = stream.getNextEntry()) {
                var file = projectDir.resolve(entry.getName()).normalize();
                if (!file.startsWith(projectDir)) {
                    throw new IOException("Zip entry escapes the project folder: " + entry.getName());
                }
                Files.createDirectories(file.getParent());
                Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // The registry record makes the folder a registered local project: folders without a record
        // are deleted at the first workspace load.
        var metainfo = userDir.resolve(".metainfo");
        Files.createDirectories(metainfo);
        Files.writeString(metainfo.resolve(projectName + ".properties"),
                "format-version=1\nrepository-id=local\n");
    }

    static void seed(Path userDir, String projectName, Path zip, String rulesXml) throws IOException {
        seed(userDir, projectName, zip);
        Files.writeString(userDir.resolve(projectName).resolve("rules.xml"), rulesXml);
    }
}
