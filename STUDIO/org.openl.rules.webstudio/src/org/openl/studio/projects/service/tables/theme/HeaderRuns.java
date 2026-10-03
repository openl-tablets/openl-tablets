package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.List;

import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedRun;
import org.openl.studio.projects.service.tables.write.VocabularyTableWriter;

/**
 * Splits the text of a Datatype header into the pieces the theme formats.
 *
 * <p>The header reads as the compiler reads it: the keyword, the name of the type, and what follows the name.
 * What follows is the type of the values of a Vocabulary, such as {@code <String>}, or the parent a Datatype
 * extends. The name ends at a space or at the {@code <} that opens the type of a Vocabulary.
 *
 * <p>The spaces between the pieces keep the look of the header cell. The pieces together cover the whole text.
 */
final class HeaderRuns {

    private HeaderRuns() {
    }

    /**
     * Splits a header text into pieces.
     *
     * @param text   the text of the header cell
     * @param cell   the look of the header cell, which every piece starts from
     * @param header the look of each part of the header
     * @return the pieces of the text, or an empty list when the text has no keyword
     */
    static List<ThemedRun> split(String text, ThemeStyle cell, TableTheme.Header header) {
        var runs = new ArrayList<ThemedRun>();
        var at = piece(runs, 0, spaces(text, 0), cell);
        var keywordEnd = word(text, at, false);
        if (keywordEnd == at) {
            return List.of();
        }
        at = piece(runs, at, keywordEnd, cell.with(header.keyword()));
        at = piece(runs, at, spaces(text, at), cell);
        at = piece(runs, at, word(text, at, true), cell.with(header.name()));
        at = piece(runs, at, spaces(text, at), cell);
        piece(runs, at, text.length(), cell.with(header.type()));
        return List.copyOf(runs);
    }

    /** Adds the piece between two indexes, when it is not empty, and answers where the next piece starts. */
    private static int piece(List<ThemedRun> runs, int start, int end, ThemeStyle style) {
        if (end > start) {
            runs.add(new ThemedRun(start, end, style));
        }
        return end;
    }

    /** The index after the spaces starting at an index. */
    private static int spaces(String text, int from) {
        var at = from;
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
            at++;
        }
        return at;
    }

    /** The index after the word starting at an index; a name also ends where the type of a Vocabulary opens. */
    private static int word(String text, int from, boolean name) {
        var at = from;
        while (at < text.length() && !Character.isWhitespace(text.charAt(at))
                && !(name && text.charAt(at) == VocabularyTableWriter.TYPE_OPEN)) {
            at++;
        }
        return at;
    }
}
