package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How wide a table search reaches.
 *
 * <p>The wider the scope, the longer the answer takes: a module is compiled by opening it, while a project and
 * everything it depends on have to be compiled through before they can be searched.
 *
 * @author Vladyslav Pikus
 */
@Schema(description = "How wide a table search reaches")
public enum TableSearchScope {

    @Schema(description = "Only the tables of the module the search names.")
    @JsonProperty("module")
    MODULE,

    @Schema(description = "Every table of the project, whichever module it is written in.")
    @JsonProperty("project")
    PROJECT,

    @Schema(description = "Every table the workspace has compiled, the projects this one depends on included.")
    @JsonProperty("all")
    ALL
}
