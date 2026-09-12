package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * One group of table properties — "Business Dimension", "Dev", and the rest the property dictionary names.
 *
 * @param name       name of the group, as the property dictionary spells it
 * @param properties properties of the group that apply to the table
 * @author Vladyslav Pikus
 */
@Builder
public record TablePropertyGroupView(
        @Parameter(description = "Name of the group the properties belong to")
        String name,

        @Parameter(description = "Properties of this group that apply to the table")
        List<TablePropertyDetailView> properties
) {
}
