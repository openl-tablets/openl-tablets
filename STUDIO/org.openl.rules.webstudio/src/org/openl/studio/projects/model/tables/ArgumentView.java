package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Arguments model for executable tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = ArgumentView.Builder.class)
public class ArgumentView {

    @Schema(description = "Name of the argument")
    public final String name;

    @Schema(description = "Data type of the argument (e.g., String, Integer, etc.)")
    public final String type;

    private ArgumentView(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String name;
        private String type;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public ArgumentView build() {
            return new ArgumentView(this);
        }
    }

}
