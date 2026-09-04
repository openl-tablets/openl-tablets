package org.openl.rules.openapi.impl;

import io.swagger.v3.oas.models.media.MediaType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MediaTypeInfo {

    @Getter
    private final MediaType content;
    @Getter
    private final String type;
}
