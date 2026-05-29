package org.openl.studio.common.projection.test;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Nested DTO used by the field projection tests to verify how nested objects are projected.
 *
 * <p>A nested field is kept whole by default -- selecting {@code owner} returns {@link UserTestView}
 * in full. Selecting {@code owner(login)} sub-projects this DTO to just {@code login}; selecting
 * {@code owner(login,email)} keeps both fields. An empty nested selection (e.g. {@code owner()}) is
 * treated as a leaf -- equivalent to selecting {@code owner} alone.
 *
 * <p>Unselected sibling fields of the parent are dropped as usual; the nested selection only governs
 * which fields of this DTO survive when {@code owner} itself is included.
 */
@Getter
@RequiredArgsConstructor
public class UserTestView {

    private final String login;
    private final String email;
}
