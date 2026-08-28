package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/** A single representable, non-null raw cell array value. */
@ArraySchema(minItems = 1,
        maxItems = 1,
        schema = @Schema(pattern = CellValueSingletonArraySchema.REPRESENTABLE_STRING,
                oneOf = {String.class, Number.class, Boolean.class}))
interface CellValueSingletonArraySchema {

    String REPRESENTABLE_STRING = "^\\S(?:[\\s\\S]*\\S)?$";
}
