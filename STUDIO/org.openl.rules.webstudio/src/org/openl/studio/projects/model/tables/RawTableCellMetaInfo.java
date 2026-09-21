package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * What the compiler knows about one cell, beside what the cell says.
 *
 * <p>This is the knowledge the Editor draws a table with: the pieces of the cell's text that refer to
 * something, the type the cell holds, whether it is the cell a decision table returns, and the editor the cell
 * asks for. It is read with the table rather than through a call of its own, so it always describes the very
 * cells that were returned.
 *
 * @param usages    the pieces of the cell's text the compiler resolved, in the order they appear
 * @param type      the type the cell holds, absent when the compiler has none for it
 * @param returnCell {@code true} when this is the cell a decision table returns; absent otherwise
 * @param editor    the editor the cell asks for, absent when the compiler names none
 * @author Vladyslav Pikus
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RawTableCellMetaInfo(
        @Parameter(description = """
                Pieces of the cell's text that refer to something the compiler knows, in the order they \
                appear. Ranges are over the cell's value as text.""")
        List<RawTableCellUsage> usages,

        @Parameter(description = "Type the cell holds, as the compiler names it")
        @Nullable String type,

        @Parameter(description = "Present and true when this is the cell a decision table returns")
        @Nullable Boolean returnCell,

        @Parameter(description = """
                Editor the cell asks for — 'text', 'numeric', 'combo', 'date', 'multiselect', 'formula', \
                'boolean', 'array', 'range', 'integer', 'double', 'multiline'.""")
        @Nullable String editor
) {

    /** Whether the compiler had nothing to say about the cell, in which case nothing is reported for it. */
    @JsonIgnore
    public boolean isEmpty() {
        return (usages == null || usages.isEmpty()) && type == null && returnCell == null && editor == null;
    }
}
