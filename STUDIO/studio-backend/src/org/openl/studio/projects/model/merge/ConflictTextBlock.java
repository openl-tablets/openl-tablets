package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.git.text.TextMerge;

/**
 * A part of a conflicted text file: lines the merge took by itself, or lines the branches changed in different ways.
 *
 * <p>A conflict holds the lines of every version. A merged part holds the lines of every version too when a branch
 * changed them; lines no branch changed are told once, as the text of the part.
 *
 * <p>Every text keeps its line breaks. A version that holds no line in the place of the part is an empty text, never
 * left out.
 */
@Schema(description = "A part of a conflicted text file: lines the merge took by itself, or a conflict")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConflictTextBlock(
        @Parameter(description = "What the part is")
        ConflictTextBlockKind kind,

        @Parameter(description = "The lines the merge took, with their line breaks; for a merged part")
        @Nullable String text,

        @Parameter(description = "The lines of your version, with their line breaks; for a conflict, and for a merged "
                + "part a branch changed")
        @Nullable String ours,

        @Parameter(description = "The lines both branches started from, with their line breaks; for a conflict, and "
                + "for a merged part a branch changed. Empty when the branches added the lines")
        @Nullable String base,

        @Parameter(description = "The lines of their version, with their line breaks; for a conflict, and for a merged "
                + "part a branch changed")
        @Nullable String theirs
) {

    static ConflictTextBlock of(TextMerge.Block block) {
        return switch (block) {
            case TextMerge.Merged merged when merged.changed() -> new ConflictTextBlock(ConflictTextBlockKind.MERGED,
                    merged.text(),
                    merged.ours(),
                    merged.base(),
                    merged.theirs());
            case TextMerge.Merged merged -> new ConflictTextBlock(ConflictTextBlockKind.MERGED,
                    merged.text(),
                    null,
                    null,
                    null);
            case TextMerge.Conflict(String ours, String base, String theirs) -> new ConflictTextBlock(
                    ConflictTextBlockKind.CONFLICT,
                    null,
                    ours,
                    base,
                    theirs);
        };
    }
}
