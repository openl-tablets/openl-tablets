package org.openl.ie.constrainer;

/**
 * Signals that a search has taken more steps than a check of a decision table may take, so the check stops without
 * a result: the parameters take too many values to try them all.
 *
 * @author Yury Molchan
 */
public final class SearchLimitExceeded extends RuntimeException {

    SearchLimitExceeded(long steps) {
        super("The search stopped after %d steps.".formatted(steps), null, false, false);
    }
}
