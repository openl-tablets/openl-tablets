package org.openl.studio.projects.service.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageFileTest {

    @TempDir
    Path folder;

    @Test
    void readsTheMessagesAsTheEngineDoesWithTheBytesEachTakes() {
        var text = """
                # a comment = no key
                greeting = Hello, {0}!
                ! another comment
                multi = line one \\
                    line two
                only key without separator

                Ключ = Текст
                """;
        var file = read(text, StandardCharsets.UTF_8);

        assertFalse(file.legacy());
        assertEquals("\n", file.lineBreak());
        assertEquals(List.of("greeting", "multi", "only key without separator", "Ключ"),
                file.messages().stream().map(MessageFile.Message::key).toList());
        assertEquals("line one line two", file.texts().get("multi"));
        assertNull(file.texts().get("only key without separator"));
        assertTrue(file.texts().containsKey("only key without separator"));

        var bytes = text.getBytes(StandardCharsets.UTF_8);
        var multi = file.messages().get(1);
        assertEquals("multi = line one \\\n    line two", slice(bytes, multi.start(), multi.contentEnd()));
        assertEquals("multi = line one \\\n    line two\n", slice(bytes, multi.start(), multi.end()));
        var russian = file.messages().get(3);
        assertEquals("Ключ = Текст", slice(bytes, russian.start(), russian.contentEnd()));
    }

    @Test
    void keepsTheLineBreakOfTheFileAndTheLastKeyWritten() {
        var file = read("a = 1\r\nb = 2\r\na = 3", StandardCharsets.UTF_8);

        assertEquals("\r\n", file.lineBreak());
        assertEquals(Map.of("a", "3", "b", "2"), file.texts());
        var last = file.messages().get(2);
        // The last line has no line break to keep.
        assertEquals(last.contentEnd(), last.end());
    }

    @Test
    void readsAFileThatIsNotUtf8InTheLegacyEncodingOfWindows() {
        var file = read("price = 5 €", MessageFile.WINDOWS_1252);

        assertTrue(file.legacy());
        assertEquals("5 €", file.texts().get("price"));
    }

    @Test
    void readsAMessageContinuedPastTheEndOfTheFile() {
        var file = read("a = one \\", StandardCharsets.UTF_8);

        assertEquals(List.of("a"), List.copyOf(file.texts().keySet()));
        assertEquals(9, file.messages().getFirst().end());
    }

    private MessageFile read(String text, Charset charset) {
        try {
            var path = folder.resolve("message.properties");
            Files.write(path, text.getBytes(charset));
            return MessageFile.read(path, "");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String slice(byte[] bytes, long start, long end) {
        return new String(bytes, (int) start, (int) (end - start), StandardCharsets.UTF_8);
    }
}
