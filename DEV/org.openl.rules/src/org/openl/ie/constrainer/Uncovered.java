package org.openl.ie.constrainer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An input that no rule of a decision table covers.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Uncovered {

    /**
     * The names of the decision table parameters.
     */
    private final String[] solutionNames;

    /**
     * The values of the parameters.
     */
    private final int[] solutionValues;
}
