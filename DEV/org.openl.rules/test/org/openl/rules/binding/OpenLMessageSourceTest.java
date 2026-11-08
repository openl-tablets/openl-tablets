package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenLMessageSourceTest {

    @TempDir
    Path folder;

    @Test
    void readsTheFilesAsTheyAreNowOnceTheMessagesAreDropped() throws IOException {
        Files.createDirectories(folder.resolve("i18n"));
        var file = folder.resolve("i18n/message.properties");
        Files.writeString(file, "greeting = Hello\n");
        try (var loader = new URLClassLoader(new URL[] {folder.toUri().toURL()}, null)) {
            var source = new OpenLMessageSource(loader);
            assertEquals("Hello", source.getMessageBundle(Locale.ROOT).msg("greeting"));

            Files.writeString(file, "greeting = Hi\n");
            // The messages read are kept for as long as the source lives.
            assertEquals("Hello", source.getMessageBundle(Locale.ROOT).msg("greeting"));

            source.reload();

            assertEquals("Hi", source.getMessageBundle(Locale.ROOT).msg("greeting"));
        }
    }
}
