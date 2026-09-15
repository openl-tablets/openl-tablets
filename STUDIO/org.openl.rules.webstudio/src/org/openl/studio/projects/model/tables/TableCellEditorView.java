package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * One way of entering a value, with everything that way of entering it needs.
 *
 * <p>Which fields are carried depends on {@code editor}: a {@code combo} and a {@code multiselect} carry the values
 * to choose from, a {@code numeric} the bounds of its type, an {@code array} how its entries are written, and a
 * {@code range} how its bounds are entered. The rest carry nothing beyond the name.
 *
 * @param editor          the kind of editor: {@code combo}, {@code multiselect}, {@code numeric}, {@code array},
 *                        {@code range}, {@code date} or {@code boolean}
 * @param choices         values to choose from, for {@code combo} and {@code multiselect}
 * @param displayValues   what to show for each choice, in the same order as {@code choices}
 * @param separator       what separates the chosen values in the cell, for {@code multiselect} and {@code array}
 * @param separatorEscaper what precedes a separator that belongs to a value, for {@code multiselect}
 * @param min             smallest value the type holds, for {@code numeric}
 * @param max             largest value the type holds, for {@code numeric}
 * @param intOnly         {@code true} when only whole numbers are accepted, for {@code numeric} and {@code array}
 * @param entryEditor     the editor one entry is written with, for {@code array} and {@code range}
 * @author Vladyslav Pikus
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One way of entering a value, with what that way of entering it needs")
public record TableCellEditorView(
        @Schema(description = """
                Kind of editor: 'combo', 'multiselect', 'numeric', 'array', 'range', 'date' or 'boolean'.""")
        String editor,

        @Schema(description = "Values to choose from; carried by 'combo' and 'multiselect'")
        @Nullable List<String> choices,

        @Schema(description = "What to show for each choice, in the order of `choices`")
        @Nullable List<String> displayValues,

        @Schema(description = "What separates the chosen values in the cell; carried by 'multiselect' and 'array'")
        @Nullable String separator,

        @Schema(description = "What precedes a separator that belongs to a value; carried by 'multiselect'")
        @Nullable String separatorEscaper,

        @Schema(description = "Smallest value the cell's type holds; carried by 'numeric'")
        @Nullable Number min,

        @Schema(description = "Largest value the cell's type holds; carried by 'numeric'")
        @Nullable Number max,

        @Schema(description = "true when only whole numbers are accepted; carried by 'numeric' and 'array'")
        @Nullable Boolean intOnly,

        @Schema(description = "Editor one entry is written with; carried by 'array' and 'range'")
        @Nullable String entryEditor
) {
}
