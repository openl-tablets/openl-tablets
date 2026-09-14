package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The properties to write onto a table.
 * <p>
 * Only the properties named here are touched: one carrying a value is written, one carrying none is taken away,
 * and a property the table declares that is not named is left as it stands. The body of the table takes no part in
 * this, so a table of any size is edited by sending the values alone.
 *
 * @param properties the properties to write, each with the text its value is written as
 * @author Vladyslav Pikus
 */
public record TablePropertiesUpdate(
        @Parameter(description = "Properties to write. A property with no value is removed from the table; one the "
                + "table declares and this list does not name is left as it stands.")
        @NotNull
        @Valid
        List<TableProperty> properties
) {
}
