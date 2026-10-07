package org.openl.studio.common.model;

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

    /**
     * A superset of {@link Full} for single-resource responses that also carry heavy, detail-only
     * collections (e.g. the per-message compilation list). List responses use {@link Full} and omit
     * those collections from both the payload and the OpenAPI schema.
     */
    public interface Detailed extends Full {
    }

    public interface Short {
    }

    public interface CreateOrUpdate {
    }

}
