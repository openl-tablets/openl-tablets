package org.openl.rules.openapi.impl;

import io.swagger.v3.oas.models.media.Schema;

@FunctionalInterface
interface OpenAPISchemaExplorer {
    void explore(Schema schema);
}
