package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Value model for vocabulary tables
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = VocabularyValueView.Builder.class)
public class VocabularyValueView {

    @Schema(description = "Value of the vocabulary entry")
    public final Object value;

    private VocabularyValueView(Builder builder) {
        this.value = builder.value;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Object value;

        private Builder() {
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public VocabularyValueView build() {
            return new VocabularyValueView(this);
        }
    }

}
