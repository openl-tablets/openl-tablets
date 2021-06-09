/**
 * Created Oct 31, 2005
 */
package org.openl.rules.helpers;

import java.util.Objects;

/**
 * @author snshor
 *
 */
public class RoundingOperator {

    interface IRoundingOperator {
        String[] getNames();

        double value(double value, double precision);
    }

    static class OpDown implements IRoundingOperator {

        @Override
        public String[] getNames() {
            return new String[] { "DOWN" };
        }

        @Override
        public double value(double value, double precision) {
            return Math.floor(value / precision) * precision;
        }
    }

    static class OpRound implements IRoundingOperator {

        @Override
        public String[] getNames() {
            return new String[] { "ROUND" };
        }

        @Override
        public double value(double value, double precision) {
            return Math.round(value / precision) * precision;
        }
    }

    static class OpUp implements IRoundingOperator {

        @Override
        public String[] getNames() {
            return new String[] { "UP" };
        }

        @Override
        public double value(double value, double precision) {
            return Math.ceil(value / precision) * precision;
        }
    }

    static final OpUp OP_UP = new OpUp();

    static final OpDown OP_DOWN = new OpDown();

    static final OpRound OP_ROUND = new OpRound();

    public static final IRoundingOperator[] OPERATORS = { OP_UP, OP_DOWN, OP_ROUND };

    final IRoundingOperator operator;

    static IRoundingOperator findOperator(String op) {
        Objects.requireNonNull(op, "RoundingOperator's name must not be null.");
        for (IRoundingOperator iRoundingOperator : OPERATORS) {

            for (int j = 0; j < iRoundingOperator.getNames().length; j++) {
                if (iRoundingOperator.getNames()[j].equalsIgnoreCase(op)) {
                    return iRoundingOperator;
                }
            }
        }
        throw new RuntimeException("Unknown RoundingOperator name: " + op);
    }

    public RoundingOperator(String op) {

        operator = findOperator(op);
    }

    public double op(double value, double precision) {
        return operator.value(value, precision);
    }

    @Override
    public String toString() {
        return operator.getNames()[0];
    }

}
