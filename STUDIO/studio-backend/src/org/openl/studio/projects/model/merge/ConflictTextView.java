package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.repository.git.text.TextMerge;

/**
 * A conflicted text file as the merge reads it, line by line: the lines it took by itself, and the conflicts between
 * them, in the order of the file.
 *
 * <p>The texts of the parts put together, one version taken of each conflict, give the whole file.
 */
@Schema(description = "A conflicted text file merged line by line")
public record ConflictTextView(
        @Parameter(description = "The parts of the file in their order: lines merged by themselves and conflicts")
        List<ConflictTextBlock> blocks
) {

    public static ConflictTextView of(List<TextMerge.Block> blocks) {
        return new ConflictTextView(blocks.stream().map(ConflictTextBlock::of).toList());
    }
}
