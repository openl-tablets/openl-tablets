/**
 * Created Oct 31, 2005
 */
package org.openl.rules.helpers;

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
            return (Math.round(value / precision)) * precision;
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

    IRoundingOperator operator;

    static IRoundingOperator findOperator(String op) {
        if (op == null) {
            throw new RuntimeException("RoundingOperator's name must not be null");
        }

        for (int i = 0; i < OPERATORS.length; i++) {

            for (int j = 0; j < OPERATORS[i].getNames().length; j++) {
                if (OPERATORS[i].getNames()[j].equalsIgnoreCase(op)) {
                    return OPERATORS[i];
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
