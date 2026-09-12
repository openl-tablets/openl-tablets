package org.openl.studio.projects.model.tables;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

/**
 * What a table says about itself besides its cells: its name and the properties that apply to it.
 *
 * <p>The properties are the ones the Table Details panel has always shown — those written on the table and
 * those it inherits from the properties table of its module or category. They arrive grouped and in the order
 * the property dictionary declares them, so a screen draws them without deciding anything.
 *
 * @param name   name of the table, as its header spells it
 * @param groups properties that apply to the table, by the group they belong to
 * @author Vladyslav Pikus
 */
@Builder
public record TableDetailsView(
        @Parameter(description = "Name of the table")
        String name,

        @Parameter(description = """
                Properties that apply to the table, grouped as the property dictionary groups them. \
                Empty for a kind of table that carries no properties.""")
        List<TablePropertyGroupView> groups
) {
}
