package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identifier of a table affected by a write operation.
 * <p>
 * The identifier differs from the one passed in the request when the write renamed the table or changed its
 * {@code id} property. A table the write moved to make room keeps its identifier.
 *
 * @author Vladyslav Pikus
 */
public record TableIdView(
        @Schema(description = "New table ID")
        String id
) {
}
