package org.openl.studio.common.projection.test;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Test DTO with per-property {@code @JsonView} annotations, used to verify that field projection composes
 * correctly with serialization views: the response should be the intersection of what the active view
 * allows and what {@code ?fields=} selects.
 */
public class ViewedTestView {

    @JsonView({Views.Public.class, Views.Full.class})
    private final String id;

    @JsonView({Views.Public.class, Views.Full.class})
    private final String name;

    @JsonView(Views.Full.class)
    private final String status;

    public ViewedTestView(String id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
