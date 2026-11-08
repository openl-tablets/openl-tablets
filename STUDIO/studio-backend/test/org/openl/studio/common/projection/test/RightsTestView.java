package org.openl.studio.common.projection.test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.openl.studio.common.access.Capabilities;

/**
 * Response DTO used by the field projection tests: its rights hold the base {@link Capabilities} unwrapped, the way
 * the capabilities of a project do.
 *
 * @param id     the id
 * @param rights the rights, the base ones unwrapped among them
 */
public record RightsTestView(String id, Rights rights) {

    /**
     * @param base    the base capabilities, written among the others
     * @param canSave a right of its own
     */
    public record Rights(@JsonUnwrapped Capabilities base, boolean canSave) {
    }
}
