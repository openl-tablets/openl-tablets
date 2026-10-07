package org.openl.rules.spring.openapi.app150;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * DTO covering {@link JsonView} schema generation with view inheritance: {@link View.Extended} extends
 * {@link View.Base}, so a field bound to the extended view must be omitted from the base-view schema but
 * present in the extended-view schema, while an unannotated field appears in every view.
 */
public record Widget(
        Long id,

        @JsonView(View.Base.class)
        String name,

        @JsonView(View.Extended.class)
        List<String> details) {

    public interface View {
        interface Base {
        }

        interface Extended extends Base {
        }
    }
}
