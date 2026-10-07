package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/**
 * A single table property: its name and its value as a string.
 * <p>
 * A date is written in ISO-8601 — {@code 2009-01-01}, with the time after it only when the value carries one — so
 * the same text is read the same way whatever the reader's locale. A property closing a period, which the engine
 * keeps at the close of the day it names, is written as that day. Every other value is written as the Table Details
 * editor shows it.
 * <p>
 * When a copy is created, the value is read back as the property it names, so a typed property (a date, an
 * enumeration) is stored as such. A blank value removes the property from the copy.
 *
 * @param name  property name
 * @param value property value; a blank value removes the property
 * @author Vladyslav Pikus
 */
public record TableProperty(
        @Parameter(description = "Property name")
        @NotBlank
        String name,

        @Parameter(description = "Property value. A date is written in ISO-8601, any other value as the Table "
                + "Details editor shows it. A blank value removes the property.")
        @Nullable String value
) {
}
