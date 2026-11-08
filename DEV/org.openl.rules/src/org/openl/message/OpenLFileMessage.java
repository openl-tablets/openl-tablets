package org.openl.message;

import java.nio.file.Path;
import java.util.List;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.source.SourceRange;
import org.openl.util.TextRange;

/**
 * A warning about a place in a file of the project that is not a workbook, such as a localization file.
 *
 * <p>The message tells the file, the line, and the key and the language it is about. A message about the whole
 * file names no key. Its source location is the address of the file, which no workbook table answers.
 *
 * <p>A message about pieces of the text of the key, such as the full-width commas of a placeholder, tells each of
 * them: a piece holds the text and where it begins and ends in it. Its source range is the first piece.
 *
 * <p>Where the message is in the file is told too, so an editor of the file marks it: each piece where it is
 * written, escapes and continued lines included, or the key for a message about the text as a whole. A message about
 * the whole file is in no place of it.
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

    /** The pieces of the text of the key the message is about, in the order of the text; empty for none. */
    private final List<SourceRange> pieces;

    /** Where the message is in the file, a place for each piece or the key alone; empty for the whole file. */
    private final List<TextRange> places;

    /**
     * @param summary  what is wrong
     * @param file     the file the message is about
     * @param line     the line of the file, the first being 1
     * @param key      the key of the file the message is about, or {@code null} for the whole file
     * @param language the language the file is written in, empty for the default one
     */
    public OpenLFileMessage(String summary, Path file, int line, @Nullable String key, String language) {
        this(summary, file, line, key, language, List.of(), List.of());
    }

    /**
     * @param summary  what is wrong
     * @param file     the file the message is about
     * @param line     the line of the file, the first being 1
     * @param key      the key of the file the message is about, or {@code null} for the whole file
     * @param language the language the file is written in, empty for the default one
     * @param pieces   the pieces of the text of the key the message is about; empty for none
     * @param places   where the message is in the file; empty for the whole file
     */
    public OpenLFileMessage(String summary,
                            Path file,
                            int line,
                            @Nullable String key,
                            String language,
                            List<SourceRange> pieces,
                            List<TextRange> places) {
        super(summary, Severity.WARN);
        this.file = file;
        this.line = line;
        this.key = key;
        this.language = language;
        this.pieces = List.copyOf(pieces);
        this.places = List.copyOf(places);
    }

    @Override
    public String getSourceLocation() {
        return file.toUri().toString();
    }

    @Override
    public @Nullable SourceRange getSourceRange() {
        return pieces.isEmpty() ? null : pieces.getFirst();
    }
}
