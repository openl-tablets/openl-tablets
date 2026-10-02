package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.IntEvent.MAX;
import static org.openl.ie.constrainer.IntEvent.MIN;
import static org.openl.ie.constrainer.IntEvent.VALUE;

import java.util.Arrays;

/**
 * The sum of the terms.
 * <p>
 * Two variables keep the bounds of the sum: the expression domain follows the bounds of the terms, and the constraint
 * domain also takes the bounds that the constraints set on the sum. The observers of the expression observe the
 * constraint domain. A change of the expression domain narrows the terms to fit the constraint domain.
 */
final class IntExpAddArray extends IntExp {

    private final IntExp[] terms;
    private final IntVar constraintDomain;
    private final IntVar expressionDomain;

    private IntExpAddArray(Constrainer constrainer, IntExp[] terms) {
        super(constrainer);
        this.terms = terms;
        var min = Arrays.stream(terms).mapToInt(IntExp::min).sum();
        var max = Arrays.stream(terms).mapToInt(IntExp::max).sum();
        constraintDomain = new IntVar(constrainer, min, max, "");
        expressionDomain = new IntVar(constrainer, min, max, "") {
            @Override
            void propagate() throws Failure {
                fitTerms();
            }
        };
        Observer observer = event -> {
            expressionDomain.setMin(expressionDomain.min() + event.minDiff());
            expressionDomain.setMax(expressionDomain.max() + event.maxDiff());
            constraintDomain.setMin(expressionDomain.min());
            constraintDomain.setMax(expressionDomain.max());
        };
        for (var term : terms) {
            term.attachObserver(MIN | MAX | VALUE, observer);
        }
    }

    /**
     * Returns the expression for the sum of the terms.
     */
    static IntExp sum(Constrainer constrainer, IntExp[] terms) {
        return switch (terms.length) {
            case 0 -> new IntExpConst(constrainer, 0);
            case 1 -> terms[0];
            case 2 -> terms[0].add(terms[1]);
            default -> new IntExpAddArray(constrainer, terms);
        };
    }

    @Override
    void attachObserver(int mask, Observer observer) {
        constraintDomain.attachObserver(mask, observer);
    }

    @Override
    int min() {
        return constraintDomain.min();
    }

    @Override
    int max() {
        return constraintDomain.max();
    }

    @Override
    void setMin(int min) throws Failure {
        if (min <= constraintDomain.min()) {
            return;
        }
        constraintDomain.setMin(min);
        var maxSum = expressionDomain.max();
        for (var term : terms) {
            var termMin = min - (maxSum - term.max());
            if (termMin > term.min()) {
                term.setMin(termMin);
            }
        }
    }

    @Override
    void setMax(int max) throws Failure {
        if (max >= constraintDomain.max()) {
            return;
        }
        constraintDomain.setMax(max);
        var minSum = expressionDomain.min();
        for (var term : terms) {
            var termMax = max - (minSum - term.min());
            if (termMax < term.max()) {
                term.setMax(termMax);
            }
        }
    }

    @Override
    void setValue(int value) throws Failure {
        if (constraintDomain.min() == value && constraintDomain.max() == value) {
            return;
        }
        constraintDomain.setValue(value);
        fitTerms(value, value, expressionDomain.min(), expressionDomain.max());
    }

    /**
     * Narrows the terms so that their sum fits the constraint domain.
     */
    private void fitTerms() throws Failure {
        var minC = constraintDomain.min();
        var maxC = constraintDomain.max();
        var minE = expressionDomain.min();
        var maxE = expressionDomain.max();
        if (minC != minE || maxC != maxE) {
            fitTerms(minC, maxC, minE, maxE);
        }
    }

    /**
     * Narrows every term so that the sum of the terms fits the range from {@code minC} to {@code maxC}, given that
     * it ranges from {@code minE} to {@code maxE}.
     */
    private void fitTerms(int minC, int maxC, int minE, int maxE) throws Failure {
        for (var term : terms) {
            var min = term.min();
            var max = term.max();
            var newMin = minC - (maxE - max);
            if (newMin > min) {
                term.setMin(newMin);
            }
            var newMax = maxC - (minE - min);
            if (newMax < max) {
                term.setMax(newMax);
            }
        }
    }
}
