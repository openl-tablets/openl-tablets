package org.openl.studio.common.projection.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Response DTO used by the field projection tests. Includes a {@link JsonIgnore} and a
 * {@code WRITE_ONLY} property to verify that projection can never expose normally hidden fields, plus a
 * nested object ({@code owner}) and a nested array of objects ({@code members}) for nested selection.
 */
@Getter
@RequiredArgsConstructor
public class ProjectTestView {

    private final String id;
    private final String name;
    private final String status;

    @JsonIgnore
    private final String secret;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final String writeOnly;

    private final UserTestView owner;

    private final List<UserTestView> members;
}
