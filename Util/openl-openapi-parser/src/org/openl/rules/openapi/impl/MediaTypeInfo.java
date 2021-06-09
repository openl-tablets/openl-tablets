package org.openl.rules.openapi.impl;

import io.swagger.v3.oas.models.media.MediaType;

public class MediaTypeInfo {

    private final MediaType content;
    private final String type;

    public MediaTypeInfo(MediaType content, String type) {
        this.content = content;
        this.type = type;
    }

    public MediaType getContent() {
        return content;
    }

    public String getType() {
        return type;
    }
}
