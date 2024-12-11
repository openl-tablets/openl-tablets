package org.openl.rules.rest.settings.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.webstudio.web.admin.RepositoryType;

@JsonDeserialize(builder = DeployConfigRepositoryConfigurationModel.Builder.class)
public class DeployConfigRepositoryConfigurationModel {

    @Parameter(description = "Deploy configuration repository name", required = true)
    @NotBlank
    private final String name;

    @Parameter(description = "Deploy configuration repository type")
    private final RepositoryType type;

    @Parameter(description = "Use design repository for deploy config")
    private final String useDesignRepositoryForDeployConfig;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = RepositorySettings.class)
    private final JsonNode settings;

    private DeployConfigRepositoryConfigurationModel(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.settings = builder.settings;
        this.useDesignRepositoryForDeployConfig = builder.useDesignRepositoryForDeployConfig;
    }

    public String getName() {
        return name;
    }

    public RepositoryType getType() {
        return type;
    }

    public String getUseDesignRepositoryForDeployConfig() {
        return useDesignRepositoryForDeployConfig;
    }

    public JsonNode getSettings() {
        return settings;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String name;
        private RepositoryType type;
        private JsonNode settings;
        private String useDesignRepositoryForDeployConfig;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(RepositoryType type) {
            this.type = type;
            return this;
        }

        public Builder settings(JsonNode settings) {
            this.settings = settings;
            return this;
        }

        public Builder useDesignRepositoryForDeployConfig(String useDesignRepositoryForDeployConfig) {
            this.useDesignRepositoryForDeployConfig = useDesignRepositoryForDeployConfig;
            return this;
        }

        public DeployConfigRepositoryConfigurationModel build() {
            return new DeployConfigRepositoryConfigurationModel(this);
        }
    }
}
