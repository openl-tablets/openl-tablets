package org.openl.studio.deployment.model;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Revision of a project in the design repository")
@Builder
public class DesignRevisionViewModel {

    @Parameter(description = "Revision ID", required = true)
    public final String revision;

    @Parameter(description = "Author of the revision")
    public final String modifiedBy;

    @Parameter(description = "Date and time of the revision")
    public final ZonedDateTime modifiedAt;

}
