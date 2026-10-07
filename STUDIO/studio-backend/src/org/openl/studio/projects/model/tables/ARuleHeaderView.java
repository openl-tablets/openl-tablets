package org.openl.studio.projects.model.tables;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Decision header abstract model
 *
 * @author Vladyslav Pikus
 */
public abstract class ARuleHeaderView {

    public static final String UNKNOWN_HEADER_NAME = "col";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Title of the rule column")
    public final String title;

    protected ARuleHeaderView(Builder<?> builder) {
        this.title = builder.title;
    }

    public abstract static class Builder<T extends ARuleHeaderView.Builder<T>> {

        private String title;

        protected Builder() {
        }

        protected abstract T self();

        public T title(String title) {
            this.title = title;
            return self();
        }

        public abstract ARuleHeaderView build();
    }

}
