package org.openl.rules.rest.settings.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.openl.rules.webstudio.web.admin.RepositoryType;

@JsonDeserialize(builder = CreateRepositoryTemplateModel.Builder.class)
public class CreateRepositoryTemplateModel {

    @NotNull
    private final RepositoryType type;

    private CreateRepositoryTemplateModel(Builder builder) {
        this.type = builder.type;
    }

    public RepositoryType getType() {
        return type;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private RepositoryType type;

        private Builder() {
        }

        public Builder type(RepositoryType type) {
            this.type = type;
            return this;
        }

        public CreateRepositoryTemplateModel build() {
            return new CreateRepositoryTemplateModel(this);
        }
    }
}
