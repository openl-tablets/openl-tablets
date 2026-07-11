package org.openl.studio.deployment.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.model.ProjectIdModel;

@Schema(description = "Deployment view model")
@Builder
public class DeploymentViewModel {

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "Deployment identifier", required = true)
    public final ProjectIdModel id;

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "Deployment name", required = true)
    public final String name;

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "Production repository id", required = true)
    public final String repository;

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "List of deployment items")
    public final List<DeploymentItemViewModel> items;

}
