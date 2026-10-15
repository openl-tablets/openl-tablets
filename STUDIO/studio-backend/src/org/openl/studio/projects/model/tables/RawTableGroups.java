package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The lines of the body of a table that start a group, which a screen sets apart with a line above or on the left.
 *
 * <p>A group is the rules that share a merged value of a condition, the columns of a lookup that share a merged value
 * of a horizontal condition, or the conditions of a ColumnMatch table indented under one name.
 *
 * @param rows    the rows that start a group, in table coordinates
 * @param columns the columns that start a group, in table coordinates
 * @author Yury Molchan
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "The lines of the body of a table that start a group")
public record RawTableGroups(
        @Parameter(description = "The rows that start a group, in table coordinates; a line is drawn above each")
        List<Integer> rows,

        @Parameter(description = """
                The columns that start a group, in table coordinates, such as the columns of a lookup under a merged \
                horizontal value; a line is drawn on the left of each""")
        List<Integer> columns
) {

    /** No groups at all. */
    public static final RawTableGroups NONE = new RawTableGroups(List.of(), List.of());

    public RawTableGroups {
        rows = List.copyOf(rows);
        columns = List.copyOf(columns);
    }
}
