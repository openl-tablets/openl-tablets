package org.openl.message;

import java.nio.file.Path;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A warning about a place in a file of the project that is not a workbook, such as a localization file.
 *
 * <p>The message tells the file, the line, and the key and the language it is about. A message about the whole
 * file names no key. Its source location is the address of the file, which no workbook table answers.
 */
@Getter
public class OpenLFileMessage extends OpenLMessage {

    /** The file the message is about. */
    private final Path file;

    /** The line of the file, the first being 1. */
    private final int line;

    /** The key of the file the message is about, or {@code null} for the whole file. */
    private final @Nullable String key;

    /** The language the file is written in, empty for the default one. */
    private final String language;

    /**
     * @param summary  what is wrong
     * @param file     the file the message is about
     * @param line     the line of the file, the first being 1
     * @param key      the key of the file the message is about, or {@code null} for the whole file
     * @param language the language the file is written in, empty for the default one
     */
    public OpenLFileMessage(String summary, Path file, int line, @Nullable String key, String language) {
        super(summary, Severity.WARN);
        this.file = file;
        this.line = line;
        this.key = key;
        this.language = language;
    }

    @Override
    public String getSourceLocation() {
        return file.toUri().toString();
    }
}
