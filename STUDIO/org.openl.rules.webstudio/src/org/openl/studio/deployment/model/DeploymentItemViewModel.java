package org.openl.studio.deployment.model;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Deployment item view model")
@Builder
public class DeploymentItemViewModel {

    @Parameter(description = "Project name", required = true)
    public final String name;

    @Parameter(description = "Author of latest deploy", required = true)
    public final String modifiedBy;

    @Parameter(description = "Date and time of latest deploy", required = true)
    public final ZonedDateTime modifiedAt;

    @Parameter(description = "Revision the deployed project has in the design repository")
    public final DesignRevisionViewModel designRevision;

}
