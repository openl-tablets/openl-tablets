package org.openl.studio.compare.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * The result of comparing two Excel files: what the files hold, grouped by sheet.
 *
 * <p>Every element carries its own status, so a screen showing only what differs filters the tree
 * itself, without asking for it again.
 *
 * @param id        identifier of the comparison
 * @param identical whether the two files hold the same elements with the same content
 * @param sheets    sheets of the two files, each with the tables it holds
 */
@Builder
public record ComparisonView(

        @Schema(description = "Identifier of the comparison")
        String id,

        @Schema(description = """
                Whether the two files hold the same elements with the same content. The files may \
                still differ in name, formatting or metadata.""")
        boolean identical,

        @Schema(description = "Sheets of the two files, each with the tables it holds")
        List<ComparisonNodeView> sheets) {
}
