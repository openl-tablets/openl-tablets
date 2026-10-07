package org.openl.studio.settings.model.auth;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Getter;

import org.openl.rules.webstudio.web.admin.security.UserMode;

@JsonDeserialize(builder = CreateAuthenticationTemplateModel.Builder.class)
public class CreateAuthenticationTemplateModel {

    @Getter
    @NotNull
    private final UserMode userMode;

    private CreateAuthenticationTemplateModel(Builder builder) {
        this.userMode = builder.userMode;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private UserMode userMode;

        private Builder() {
        }

        public Builder userMode(UserMode userMode) {
            this.userMode = userMode;
            return this;
        }

        public CreateAuthenticationTemplateModel build() {
            return new CreateAuthenticationTemplateModel(this);
        }
    }
}
