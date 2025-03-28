package org.openl.rules.rest.settings.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.openl.rules.webstudio.web.admin.RepositoryType;

@JsonDeserialize(builder = DesignRepositoryConfigurationModel.Builder.class)
public class DesignRepositoryConfigurationModel {

    @NotBlank
    private final String name;

    private final RepositoryType type;

    private final String useDesignRepositoryForDeployConfig;

    @NotNull
    private final JsonNode settings;

    private DesignRepositoryConfigurationModel(Builder builder) {
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

        private Builder() {}

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

        public DesignRepositoryConfigurationModel build() {
            return new DesignRepositoryConfigurationModel(this);
        }
    }
}
