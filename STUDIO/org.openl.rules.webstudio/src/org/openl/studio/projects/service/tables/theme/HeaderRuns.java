package org.openl.studio.projects.service.tables.theme;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.studio.projects.service.tables.theme.ThemedTable.ThemedRun;
import org.openl.studio.projects.service.tables.write.VocabularyTableWriter;
import org.openl.util.StringUtils;

/**
 * Splits the text of a table header into the pieces the theme formats.
 *
 * <p>The header reads as the compiler reads it, and its keyword tells how:
 * <ul>
 *     <li>A Datatype header names the type, then what follows the name: the type of the values of a Vocabulary, such
 *     as {@code <String>}, or the parent a Datatype extends. The name ends at a space or at the {@code <} that opens
 *     the type of a Vocabulary.</li>
 *     <li>A Spreadsheet header names the type it returns, the name of the table, then its parameters in parentheses.
 *     The name is the last word before the parameters, so a header that leaves out the type names none.</li>
 * </ul>
 *
 * <p>The spaces between the pieces keep the look of the header cell. The pieces together cover the whole text.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class HeaderRuns {

    /** What opens the parameters of a Spreadsheet. */
    private static final char PARAMETERS_OPEN = '(';

    /** The text of the header cell. */
    private final String text;
    /** The look of the header cell, which every piece starts from. */
    private final ThemeStyle cell;
    /** The look of each part of the header. */
    private final TableTheme.Header header;
    /** The pieces found so far, in the order of the text. */
    private final List<ThemedRun> runs = new ArrayList<>();

    /**
     * Splits a header text into pieces.
     *
     * @param text   the text of the header cell
     * @param cell   the look of the header cell, which every piece starts from
     * @param header the look of each part of the header
     * @return the pieces of the text, or an empty list when the text has no keyword
     */
    static List<ThemedRun> split(String text, ThemeStyle cell, TableTheme.Header header) {
        return new HeaderRuns(text, cell, header).split();
    }

    private List<ThemedRun> split() {
        var at = piece(0, spaces(text, 0), cell);
        var keywordEnd = word(text, at, false);
        if (keywordEnd == at) {
            return List.of();
        }
        var keyword = text.substring(at, keywordEnd);
        at = piece(at, keywordEnd, cell.with(header.keyword()));
        at = piece(at, spaces(text, at), cell);
        if (IXlsTableNames.DATATYPE_TABLE.equals(keyword)) {
            splitType(at);
        } else {
            splitMethod(at);
        }
        return List.copyOf(runs);
    }

    /** The pieces of a Datatype header after its keyword: the name, then what follows it. */
    private void splitType(int from) {
        var at = piece(from, word(text, from, true), cell.with(header.name()));
        at = piece(at, spaces(text, at), cell);
        piece(at, text.length(), cell.with(header.type()));
    }

    /**
     * The type a Spreadsheet header names before the name of the table.
     *
     * @param text the text of the header cell
     * @return the type, or an empty text for a header that names none
     */
    static String returnType(String text) {
        var from = spaces(text, word(text, spaces(text, 0), false));
        return text.substring(from, MethodPieces.of(text, from).typeEnd());
    }

    /** The pieces of a Spreadsheet header after its keyword: the type it returns, the name, the parameters. */
    private void splitMethod(int from) {
        var pieces = MethodPieces.of(text, from);
        var at = piece(from, pieces.typeEnd(), cell.with(header.type()));
        at = piece(at, pieces.nameStart(), cell);
        at = piece(at, pieces.nameEnd(), cell.with(header.name()));
        at = piece(at, pieces.parameters(), cell);
        piece(at, text.length(), cell.with(header.parameters()));
    }

    /**
     * Where the pieces of a Spreadsheet header end after its keyword.
     *
     * @param typeEnd    the index after the type it returns
     * @param nameStart  the index of the name of the table
     * @param nameEnd    the index after the name
     * @param parameters the index of the parameters, or the end of the text when it names none
     */
    private record MethodPieces(int typeEnd, int nameStart, int nameEnd, int parameters) {

        static MethodPieces of(String text, int from) {
            var open = text.indexOf(PARAMETERS_OPEN, from);
            var parameters = open < 0 ? text.length() : open;
            var nameEnd = trimmedEnd(text, from, parameters);
            var nameStart = wordStart(text, from, nameEnd);
            return new MethodPieces(trimmedEnd(text, from, nameStart), nameStart, nameEnd, parameters);
        }

        /** The index after the text between two indexes, without the spaces it ends with. */
        private static int trimmedEnd(String text, int from, int to) {
            return Math.max(from, StringUtils.lastNonSpace(text, from, to) + 1);
        }

        /** The index where the word that ends at an index starts, not before a lower bound. */
        private static int wordStart(String text, int from, int end) {
            return Math.max(from, StringUtils.last(text, from, end, StringUtils::isSpaceOrControl) + 1);
        }
    }

    /** Adds the piece between two indexes, when it is not empty, and answers where the next piece starts. */
    private int piece(int start, int end, ThemeStyle style) {
        if (end > start) {
            runs.add(new ThemedRun(start, end, style));
        }
        return end;
    }

    /** The index after the spaces starting at an index. */
    private static int spaces(String text, int from) {
        return orEnd(text, StringUtils.firstNonSpace(text, from, text.length()));
    }

    /** The index after the word starting at an index; a name also ends where the type of a Vocabulary opens. */
    private static int word(String text, int from, boolean name) {
        return orEnd(text, StringUtils.first(text, from, text.length(),
                ch -> StringUtils.isSpaceOrControl(ch) || name && ch == VocabularyTableWriter.TYPE_OPEN));
    }

    /** The index found, or the end of the text when nothing was found. */
    private static int orEnd(String text, int found) {
        return found < 0 ? text.length() : found;
    }
}
