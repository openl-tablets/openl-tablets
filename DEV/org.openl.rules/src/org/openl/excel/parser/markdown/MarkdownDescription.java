package org.openl.excel.parser.markdown;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.StringUtils;

/**
 * The description of a table of a Markdown file: the text between the front matter and the table.
 *
 * <p>The description is the {@code description} property of the table. It is read as written, its Markdown left as it
 * is: the lines of a block of text are joined with line breaks, and the blocks with a blank line. A quote, a block of
 * lines starting with {@code >}, and an HTML comment, {@code <!-- -->}, are comments and no part of the description.
 *
 * <p>A line of a description that would start a quote, a code block or a table is written with a backslash before it,
 * {@code \>}, as Markdown escapes it, and read back without it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MarkdownDescription {

    /** The table property the description is. */
    static final String PROPERTY = "description";
    /** What a line of a description must not start with, as it would start a quote, a code block or a table. */
    private static final List<String> MARKERS = List.of(">", "```", "~~~", "|");

    /**
     * Returns the description written in blocks of the lines of a file, or {@code null} when it has none.
     */
    static @Nullable String of(List<String> lines, List<TableFile.Block> blocks) {
        var text = new StringBuilder();
        for (var block : blocks) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            for (var line = block.firstLine(); line <= block.lastLine(); line++) {
                text.append(line == block.firstLine() ? "" : "\n").append(unescaped(lines.get(line).stripTrailing()));
            }
        }
        return StringUtils.trimToNull(text.toString());
    }

    /**
     * Returns the lines a description is written in, none for no description.
     */
    static List<String> linesOf(@Nullable String description) {
        if (StringUtils.isBlank(description)) {
            return List.of();
        }
        var lines = new ArrayList<String>();
        for (var line : description.strip().replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            lines.add(escaped(line.stripTrailing()));
        }
        return lines;
    }

    private static String escaped(String line) {
        var text = line.stripLeading();
        var marked = MARKERS.stream().anyMatch(text::startsWith);
        return marked ? line.substring(0, line.length() - text.length()) + "\\" + text : line;
    }

    private static String unescaped(String line) {
        var text = line.stripLeading();
        var marked = text.startsWith("\\") && MARKERS.stream().anyMatch(marker -> text.startsWith(marker, 1));
        return marked ? line.substring(0, line.length() - text.length()) + text.substring(1) : line;
    }
}
