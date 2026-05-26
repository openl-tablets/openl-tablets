package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

@Builder
public record TableMessageSource(
        @Parameter(description = "Identifier of the table the message originates from. "
                + "Matches the id used by the table editor and tree URLs.")
        String id,

        @Parameter(description = "Display name of the table (the header cell text).")
        String name,

        @Parameter(description = "Module (workbook) the table belongs to.")
        String module,

        @Parameter(description = "Address of the cell the message points to, in A1 notation. "
                + "May be null when the message is associated with the table as a whole.")
        String cell
) implements MessageSource {
}
