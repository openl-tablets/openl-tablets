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

@JsonDeserialize(builder = CURepositoryConfigurationModel.Builder.class)
public class CURepositoryConfigurationModel {

    @Parameter(description = "Repository ID")
    private final String id;

    @Parameter(description = "Repository name", required = true)
    @NotBlank
    private final String name;

    @Parameter(description = "Repository type", required = true)
    @NotNull
    private final RepositoryType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = RepositorySettings.class)
    @NotNull
    private final JsonNode settings;

    private CURepositoryConfigurationModel(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.settings = builder.settings;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RepositoryType getType() {
        return type;
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
        private String id;
        private String name;
        private RepositoryType type;
        private JsonNode settings;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
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

        public CURepositoryConfigurationModel build() {
            return new CURepositoryConfigurationModel(this);
        }
    }
}
