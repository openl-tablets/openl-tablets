package org.openl.studio.projects.model.tables;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * How the cells of a table that declares what a part of it holds are written.
 *
 * <p>A Data or a Test table declares its columns, a decision table its conditions and its actions, a lookup
 * the cells its rules meet in. The part's editor holds for every cell of it, the ones nobody has written in
 * yet included, so a line laid down in such a table takes the type its part was declared with rather than
 * nothing at all, and a table that holds no rows or no rules at all answers all the same. A cell of its own is
 * named only where that cell is written some other way than the part it stands in.
 *
 * @author Vladyslav Pikus
 */
@Getter
@SuperBuilder
@Jacksonized
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "The editors the parts of a table declare, and the cells that ask for another")
public class DeclaredTableEditorsView extends TableEditorsView {

    public static final String KIND = "declared";

    @Builder.Default
    @Schema(description = "Parts of the table whose every cell asks for one of the editors")
    private final List<Area> areas = List.of();

    @Override
    public String getKind() {
        return KIND;
    }

    /**
     * One part of the table and the way every cell of it is written.
     *
     * <p>A part left open along an axis runs to the table's edge and on past it, so a line laid down there
     * belongs to it too: the cells of a rule added under the last one are written the way its column declares.
     *
     * @param row     0-based first row of the part in the table's matrix
     * @param column  0-based first column of the part
     * @param rows    how many rows it covers, or {@code null} for the rest of the table and beyond
     * @param columns how many columns it covers, or {@code null} for the rest of the table and beyond
     * @param editor  index into {@code editors}
     */
    // A part that runs on past the table's edge says so with a null rather than by leaving the count
    // out: a screen reading this is told how far the part reaches, not left to guess from a silence.
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(name = "TableAreaEditorRef", description = "A part of the table and the editor its cells ask for")
    public record Area(
            @Schema(description = "0-based first row of the part in the matrix the raw read returns")
            Integer row,
            @Schema(description = "0-based first column of the part in the matrix the raw read returns")
            Integer column,
            @Schema(description = "How many rows the part covers; null where it covers the rest of the table "
                    + "and every row laid down after it")
            Integer rows,
            @Schema(description = "How many columns the part covers; null where it covers the rest of the table "
                    + "and every column laid down after it")
            Integer columns,
            @Schema(description = "0-based index into `editors`")
            Integer editor
    ) {
    }
}
