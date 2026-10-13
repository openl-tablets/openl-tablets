package org.openl.rules.repository.api;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

/**
 * A workbook both branches changed, which the merge combined without asking the user.
 *
 * <p>Each sheet either branch changed is listed once: taken from the target branch, taken from the source
 * branch, or combined from both.
 *
 * @param path         the path of the workbook from the root of the repository
 * @param targetSheets the sheets taken from the branch the merge changes
 * @param sourceSheets the sheets taken from the branch merged in
 * @param bothSheets   the sheets combined from both branches
 */
public record MergedWorkbook(@NonNull String path,
                             @NonNull List<@NonNull String> targetSheets,
                             @NonNull List<@NonNull String> sourceSheets,
                             @NonNull List<@NonNull String> bothSheets) {

    public MergedWorkbook {
        Objects.requireNonNull(path);
        targetSheets = List.copyOf(targetSheets);
        sourceSheets = List.copyOf(sourceSheets);
        bothSheets = List.copyOf(bothSheets);
    }
}
