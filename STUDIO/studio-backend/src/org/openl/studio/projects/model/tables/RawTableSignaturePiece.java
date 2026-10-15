package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A piece of the header text of a table.
 *
 * <p>A piece starts where the piece before it ends, the first one at the start of the text, and takes the spaces
 * before it. The pieces together cover the header text.
 *
 * @param part the part of the header the piece is
 * @param to   the index in the header text after the last character of the piece
 * @author Yury Molchan
 */
@Schema(description = "A piece of the header text of a table")
public record RawTableSignaturePiece(
        @Parameter(description = "The part of the header the piece is")
        RawTableSignaturePart part,

        @Parameter(description = """
                The index in the header text after the last character of the piece. A piece starts where the piece \
                before it ends and takes the spaces before it.""")
        int to
) {
}
