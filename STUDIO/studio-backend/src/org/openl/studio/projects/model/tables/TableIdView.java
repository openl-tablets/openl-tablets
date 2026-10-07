package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identifier of a table affected by a write operation.
 * <p>
 * The identifier may differ from the one passed in the request when the table was relocated to a free area because it
 * had no room to grow at its original position.
 *
 * @author Vladyslav Pikus
 */
public record TableIdView(
        @Schema(description = "New table ID")
        String id
) {
}
