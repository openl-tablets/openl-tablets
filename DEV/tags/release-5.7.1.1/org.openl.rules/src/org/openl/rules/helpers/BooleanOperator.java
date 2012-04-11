/*
 * Created on Jun 24, 2004

 */
package org.openl.rules.helpers;

/**
 * @author jacob
 */

@SuppressWarnings("unchecked")
public class BooleanOperator {
    static interface ComparableOperator {
        public boolean compare(Comparable c1, Comparable c2);
    }

    static ComparableOperator LT = new ComparableOperator() {
        public boolean compare(Comparable c1, Comparable c2) {
            return c1.compareTo(c2) < 0;
        }
    };

    static ComparableOperator LE = new ComparableOperator() {
        public boolean compare(Comparable c1, Comparable c2) {
            return c1.compareTo(c2) <= 0;
        }
    };

    static ComparableOperator GE = new ComparableOperator() {
        public boolean compare(Comparable c1, Comparable c2) {
            return c1.compareTo(c2) >= 0;
        }
    };

    static ComparableOperator GT = new ComparableOperator() {
        public boolean compare(Comparable c1, Comparable c2) {
            return c1.compareTo(c2) > 0;
        }
    };

    static ComparableOperator EQ = new ComparableOperator() {
        public boolean compare(Comparable c1, Comparable c2) {
            return c1.compareTo(c2) == 0;
        }
    };

    static ComparableOperator NE = new ComparableOperator() {
        public boolean compare(Comparable c1, Comparable c2) {
            return c1.compareTo(c2) != 0;
        }
    };

    ComparableOperator operator;

    public BooleanOperator(String s) {
        if (s.equals("<")) {
            operator = LT;
        } else if (s.equals("<=")) {
            operator = LE;
        } else if (s.equals("==")) {
            operator = EQ;
        } else if (s.equals("!=")) {
            operator = NE;
        } else if (s.equals(">")) {
            operator = GT;
        } else if (s.equals(">=")) {
            operator = GE;
        } else {
            throw (new RuntimeException("Operator " + s + " is not defined"));
        }
    }

    public boolean compare(Comparable c1, Comparable c2) {
        return operator.compare(c1, c2);
    }

    public boolean compare(double i1, double i2) {
        return operator.compare(new Double(i1), new Double(i2));
    }

    public boolean compare(int i1, int i2) {
        return operator.compare(new Integer(i1), new Integer(i2));
    }

}
