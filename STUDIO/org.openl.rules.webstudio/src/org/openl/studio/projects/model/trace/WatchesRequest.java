package org.openl.studio.projects.model.trace;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The set of cells to watch, sent by the UI or an agent.
 *
 * <p>A cell is named by its {@code $...} step label (for example {@code $VehiclePriceFactor}) or its short
 * reference (for example {@code R2C3}). The set is applied on the next start, since a watch captures from
 * the beginning of a run.
 *
 * @param cells cell names or refs to watch
 */
@Schema(description = "trace.type.watches.desc")
public record WatchesRequest(
        @Schema(description = "trace.field.watches.cells.desc")
        @Nullable List<String> cells
) {

    public List<String> safeCells() {
        return cells == null ? List.of() : cells;
    }
}
