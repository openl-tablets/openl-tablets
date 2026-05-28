package org.openl.studio.common.projection.test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Nested DTO used to verify that field projection only affects the root type and leaves nested objects
 * fully serialized.
 */
@Getter
@RequiredArgsConstructor
public class UserTestView {

    private final String login;
    private final String email;
}
