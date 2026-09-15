package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * How a table is laid out on its sheet: which way round it is written, and where its data begins.
 *
 * <p>Both are read from the compiled table. Neither can be worked out from the cells: a table written the
 * other way round looks like any other table of text, and the headings above its data are only headings
 * because the compiler read them as such.
 *
 * @author Vladyslav Pikus
 */
@Builder
public record TableLayout(

        @Parameter(description = """
                Set where the table is written the other way round, so that a line of its data is a column \
                rather than a row. Absent where it is written the usual way round""")
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        boolean transposed,

        @Parameter(description = """
                The line the table's data begins on, counted from the table's own first row — or first \
                column, where the table is transposed. Everything before it is the table's headings, and \
                every line from it to the end of the table is data. A screen drawing the table without its \
                header rows counts those out of this position itself""")
        int firstDataLine) {
}
