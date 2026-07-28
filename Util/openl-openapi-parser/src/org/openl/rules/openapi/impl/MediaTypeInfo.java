package org.openl.rules.openapi.impl;

import io.swagger.v3.oas.models.media.MediaType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MediaTypeInfo {

    private final MediaType content;
    private final String type;

    public MediaType getContent() {
        return content;
    }

    public String getType() {
        return type;
    }
}
