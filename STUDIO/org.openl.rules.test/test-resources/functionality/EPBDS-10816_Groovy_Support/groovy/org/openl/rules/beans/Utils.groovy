package org.openl.rules.beans

class Utils {
    static def sumDoubles(Double a, Double b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a + b;
    }
}
