package org.openl.rules.serialization.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class NonEmptyMixIn {
}
