/*
 * Created on Jun 24, 2004

 */
package org.openl.rules.helpers;

/**
 * @author jacob, Marat Kamalov
 */

public class BooleanOperator {

    public BooleanOperator(String s) {
        if ("<".equals(s)) {
            operator = LT;
        } else if ("<=".equals(s)) {
            operator = LE;
        } else if ("==".equals(s)) {
            operator = EQ;
        } else if ("!=".equals(s)) {
            operator = NE;
        } else if (">".equals(s)) {
            operator = GT;
        } else if (">=".equals(s)) {
            operator = GE;
        } else {
            throw new RuntimeException(String.format("Operator %s is not defined.", s));
        }
    }

    public <T extends Comparable<T>> boolean compare(T c1, T c2) {
        return operator.compare(c1, c2);
    }

    public boolean compare(double i1, double i2) {
        return operator.compare(i1, i2);
    }

    public boolean compare(int i1, int i2) {
        return operator.compare(i1, i2);
    }

    interface ComparableOperator {
        <T extends Comparable<T>> boolean compare(T c1, T c2);
    }

    static final ComparableOperator LT = new ComparableOperator() {
        @Override
        public <T extends Comparable<T>> boolean compare(T c1, T c2) {
            return c1.compareTo(c2) < 0;
        }
    };

    static final ComparableOperator LE = new ComparableOperator() {
        @Override
        public <T extends Comparable<T>> boolean compare(T c1, T c2) {
            return c1.compareTo(c2) <= 0;
        }
    };

    static final ComparableOperator GE = new ComparableOperator() {
        @Override
        public <T extends Comparable<T>> boolean compare(T c1, T c2) {
            return c1.compareTo(c2) >= 0;
        }
    };

    static final ComparableOperator GT = new ComparableOperator() {
        @Override
        public <T extends Comparable<T>> boolean compare(T c1, T c2) {
            return c1.compareTo(c2) > 0;
        }
    };

    static final ComparableOperator EQ = new ComparableOperator() {
        @Override
        public <T extends Comparable<T>> boolean compare(T c1, T c2) {
            return c1.compareTo(c2) == 0;
        }
    };

    static final ComparableOperator NE = new ComparableOperator() {
        @Override
        public <T extends Comparable<T>> boolean compare(T c1, T c2) {
            return c1.compareTo(c2) != 0;
        }
    };

    ComparableOperator operator;
}
