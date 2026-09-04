package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/** Two or more representable raw cell array values, including null slots. */
@ArraySchema(minItems = 2,
        schema = @Schema(oneOf = {NullableCellStringSchema.class, Number.class, Boolean.class}))
interface CellValueMultipleArraySchema {
}

/** A string array element, or the null alternative accepted in arrays with two or more elements. */
@Schema(type = "string", nullable = true, pattern = CellValueSingletonArraySchema.REPRESENTABLE_STRING)
interface NullableCellStringSchema {
}
