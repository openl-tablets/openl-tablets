package org.openl.studio.common.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record PageRequest(
        @Schema(description = "Page offset. Default is 0")
        Integer offset,

        @Schema(description = "Page size limit. Default is 50")
        Integer limit
) {}
