package org.openl.rules.rest.model;

/**
 * Just a default maker class for {@link com.fasterxml.jackson.annotation.JsonView} annotations, to provide an ability
 * to render different OpenAPI schemas using single DTO and request/response deserialization in Controllers
 *
 * @author Vladyslav Pikus
 */
public final class GenericView {
    private GenericView() {
    }

    public interface Full {
    }

    public interface CreateOrUpdate {
    }

}
