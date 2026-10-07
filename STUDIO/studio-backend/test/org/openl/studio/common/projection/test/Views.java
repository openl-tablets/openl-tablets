package org.openl.studio.common.projection.test;

/**
 * Jackson serialization view markers used by the field projection + {@code @JsonView} interaction tests.
 */
public final class Views {

    private Views() {
    }

    public static class Public {
    }

    public static class Full extends Public {
    }
}
