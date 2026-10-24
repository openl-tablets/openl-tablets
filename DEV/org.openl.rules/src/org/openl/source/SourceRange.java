package org.openl.source;

import java.io.Serializable;

/**
 * The piece of a source that a message is about, such as the call in a cell that does not compile.
 * <p>
 * The source is the one the code was read from. For code written in a cell it is the cell itself: not the part of
 * its text after {@code =}, and not the body of a Method table that joins the texts of several cells.
 * <p>
 * The source keeps its text, so a tool that shows the source can check that it still holds that text before it marks
 * the piece.
 *
 * @param uri   the URI of the source, such as the URI of a cell of a workbook
 * @param code  the text of the source
 * @param start the position of the first character of the piece in the text
 * @param end   the position after the last character of the piece
 * @author Yury Molchan
 */
public record SourceRange(String uri, String code, int start, int end) implements Serializable {
}
