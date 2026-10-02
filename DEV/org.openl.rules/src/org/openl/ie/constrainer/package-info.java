/**
 * Checks the rules of a decision table for gaps and overlaps by a search over the values of its parameters.
 * <p>
 * Every parameter is an integer variable with a finite domain. A condition is an integer or boolean expression made
 * of the parameters, and a rule is the conjunction of the conditions in its row. {@link DTChecker} searches for an
 * input that no rule covers and for the inputs that two rules cover.
 */
@NullMarked
package org.openl.ie.constrainer;

import org.jspecify.annotations.NullMarked;
