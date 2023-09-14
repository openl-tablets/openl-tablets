package org.openl.rules.rest.model.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * TODO description
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = VocabularyValueView.Builder.class)
public class VocabularyValueView {

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
