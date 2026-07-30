package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Which project descriptor a migrate targets.")
public enum MigrationScope {
    @Schema(description = "The project descriptor rules.xml: move root workbooks under rules/ or rewrite the file.")
    @JsonProperty("rulesXml")
    RULES_XML,

    @Schema(description = "The deployment descriptor rules-deploy.xml.")
    @JsonProperty("rulesDeploy")
    RULES_DEPLOY
}
