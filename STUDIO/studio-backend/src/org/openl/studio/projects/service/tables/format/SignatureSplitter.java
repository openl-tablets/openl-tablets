package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.studio.projects.model.tables.RawTableSignaturePart;
import org.openl.studio.projects.model.tables.RawTableSignaturePiece;
import org.openl.studio.projects.service.tables.write.VocabularyTableWriter;
import org.openl.util.StringUtils;

/**
 * Splits the header text of a table into its pieces: the keyword, the type, the name and the parameters.
 *
 * <p>The header reads as the compiler reads it, and its keyword tells how:
 * <ul>
 *     <li>A Datatype header names the table, then what follows the name: the type of the values of a Vocabulary,
 *     such as {@code <String>}, or the parent a Datatype extends. The name ends at a space or at the {@code <} that
 *     opens the type of a Vocabulary.</li>
 *     <li>Any other header names the type it returns, the name of the table, then its parameters in parentheses. The
 *     name is the last word before the parameters, so a header leaving out the type names none. The type may take
 *     several words, such as {@code Collect Error[]} or the {@code <MATCH> String} of a ColumnMatch table. A Data
 *     header names the type of its rows, and a Test header the table it tests, in place of the type.</li>
 * </ul>
 *
 * <p>Each parameter is a type and a name, the last word of the parameter; the parentheses and the commas are the
 * punctuation. A comma inside the angle brackets or the square brackets of a type separates no parameters.
 *
 * <p>A piece takes the spaces before it, so the pieces together cover the whole text.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class SignatureSplitter {

    private static final char OPEN = '(';
    private static final char CLOSE = ')';
    private static final char COMMA = ',';

    private final String text;
    private final List<RawTableSignaturePiece> pieces = new ArrayList<>();

    /**
     * Splits a header text into its pieces.
     *
     * @param text the text of the header cell
     * @return the pieces, or an empty list for a text with no keyword
     */
    static List<RawTableSignaturePiece> split(String text) {
        return new SignatureSplitter(text).split();
    }

    private List<RawTableSignaturePiece> split() {
        var from = spaces(0);
        var keywordEnd = word(from, false);
        if (keywordEnd == from) {
            return List.of();
        }
        piece(keywordEnd, RawTableSignaturePart.KEYWORD);
        if (IXlsTableNames.DATATYPE_TABLE.equals(text.substring(from, keywordEnd))) {
            piece(word(spaces(keywordEnd), true), RawTableSignaturePart.NAME);
            piece(text.length(), RawTableSignaturePart.TYPE);
        } else {
            splitMethod(keywordEnd);
            // What follows the parameters, or the spaces closing the text, ends the last piece.
            piece(text.length(), pieces.getLast().part());
        }
        return List.copyOf(pieces);
    }

    /** The type, the name and the parameters of a header after its keyword. */
    private void splitMethod(int from) {
        var open = text.indexOf(OPEN, from);
        var parameters = open < 0 ? text.length() : open;
        var nameEnd = trimmedEnd(from, parameters);
        var nameStart = Math.max(from, StringUtils.last(text, from, nameEnd, StringUtils::isSpaceOrControl) + 1);
        piece(trimmedEnd(from, nameStart), RawTableSignaturePart.TYPE);
        piece(nameEnd, RawTableSignaturePart.NAME);
        if (open >= 0) {
            splitParameters(open);
        }
    }

    /** The parameters, from the parenthesis that opens them. */
    private void splitParameters(int open) {
        piece(open + 1, RawTableSignaturePart.PUNCTUATION);
        var depth = 0;
        var start = open + 1;
        for (var at = start; at < text.length(); at++) {
            var ch = text.charAt(at);
            if (ch == '<' || ch == '[') {
                depth++;
            } else if ((ch == '>' || ch == ']') && depth > 0) {
                depth--;
            } else if (depth == 0 && (ch == COMMA || ch == CLOSE)) {
                parameter(start, at);
                piece(at + 1, RawTableSignaturePart.PUNCTUATION);
                start = at + 1;
                if (ch == CLOSE) {
                    return;
                }
            }
        }
        parameter(start, text.length());
    }

    /** A parameter between two indexes: its type, then its name, the last word of it. */
    private void parameter(int from, int to) {
        var end = trimmedEnd(from, to);
        var nameStart = StringUtils.last(text, from, end, StringUtils::isSpaceOrControl) + 1;
        if (nameStart > from && nameStart > spaces(from)) {
            piece(trimmedEnd(from, nameStart), RawTableSignaturePart.PARAMETER_TYPE);
            piece(end, RawTableSignaturePart.PARAMETER_NAME);
        } else {
            piece(end, RawTableSignaturePart.PARAMETER_TYPE);
        }
    }

    /**
     * Ends a piece at an index, taking everything since the piece before it. An empty piece is left out, and spaces
     * alone or a piece of the part of the piece before it join that piece, which keeps the pieces covering the text.
     */
    private void piece(int to, RawTableSignaturePart part) {
        var from = pieces.isEmpty() ? 0 : pieces.getLast().to();
        if (to <= from) {
            return;
        }
        var joins = !pieces.isEmpty()
                && (StringUtils.isBlank(text.substring(from, to)) || pieces.getLast().part() == part);
        if (joins) {
            pieces.set(pieces.size() - 1, new RawTableSignaturePiece(pieces.getLast().part(), to));
        } else {
            pieces.add(new RawTableSignaturePiece(part, to));
        }
    }

    /** The index after the text between two indexes, without the spaces it ends with. */
    private int trimmedEnd(int from, int to) {
        return Math.max(from, StringUtils.lastNonSpace(text, from, to) + 1);
    }

    /** The index after the spaces starting at an index. */
    private int spaces(int from) {
        return orEnd(StringUtils.firstNonSpace(text, from, text.length()));
    }

    /** The index after the word starting at an index; a name also ends where the type of a Vocabulary opens. */
    private int word(int from, boolean name) {
        return orEnd(StringUtils.first(text, from, text.length(),
                ch -> StringUtils.isSpaceOrControl(ch) || name && ch == VocabularyTableWriter.TYPE_OPEN));
    }

    /** The index found, or the end of the text when nothing was found. */
    private int orEnd(int found) {
        return found < 0 ? text.length() : found;
    }
}
