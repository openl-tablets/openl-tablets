package org.openl.ie.constrainer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Two rules of a decision table that cover the same input.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Overlapping {

    /**
     * The indexes of the rules A and B, where A is applied first.
     */
    private final int[] overlapped;

    /**
     * How the rules overlap.
     */
    private final OverlappingStatus status;

    /**
     * The names of the decision table parameters.
     */
    private final String[] solutionNames;

    /**
     * The values of the parameters in an input that both rules cover.
     */
    private final int[] solutionValues;

    /**
     * How the rules A and B overlap, where A is applied first.
     */
    public enum OverlappingStatus {
        /**
         * A covers every input of B, so B is never applied. It is an error.
         */
        BLOCK,
        /**
         * A and B share some of their inputs. It is an error or the intended behavior.
         */
        PARTIAL,
        /**
         * B covers every input of A, as a default rule for all the rest does. It is the intended behavior in most
         * cases.
         */
        OVERRIDE
    }
}
