package org.openl.rules.serialization.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class NonNullMixIn {
}
