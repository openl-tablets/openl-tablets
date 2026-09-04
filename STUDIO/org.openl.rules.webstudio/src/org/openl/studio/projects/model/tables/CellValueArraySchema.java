package org.openl.studio.projects.model.tables;

import io.swagger.v3.oas.annotations.media.Schema;

/** OpenAPI representation of the one-dimensional arrays accepted as raw table cell values. */
@Schema(oneOf = {CellValueSingletonArraySchema.class, CellValueMultipleArraySchema.class})
interface CellValueArraySchema {
}

/** A string cell value or a null cell value. */
@Schema(type = "string", nullable = true)
interface NullableCellValueSchema {
}
