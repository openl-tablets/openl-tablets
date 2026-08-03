package org.openl.studio.projects.model.tables;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/**
 * A single table property: its name and value in display string form.
 * <p>
 * The value is a string, exactly as it appears in the Table Details editor. When a copy is created, the value is
 * parsed back with the property's definition, so a typed property (a date, an enumeration) is stored correctly. A blank
 * value removes the property from the copy.
 *
 * @param name  property name
 * @param value property value in display string form; a blank value removes the property
 * @author Vladyslav Pikus
 */
public record TableProperty(
        @Parameter(description = "Property name")
        @NotBlank
        String name,

        @Parameter(description = "Property value in display string form. A blank value removes the property.")
        @Nullable String value
) {
}
