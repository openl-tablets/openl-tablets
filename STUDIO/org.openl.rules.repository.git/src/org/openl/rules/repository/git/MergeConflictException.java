package org.openl.rules.repository.git;

import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Exception thrown when a merge conflict occurs during a Git merge operation.
 */
@RequiredArgsConstructor
public class MergeConflictException extends IOException {

    @Getter
    private final MergeConflictDetails details;
}
