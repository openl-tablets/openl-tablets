package org.openl.extension.xmlrules.utils;

public class ArrayOperators {
    public static Double[][] add(Object[][] x, Object[][] y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return add(a, b);
            }
        });
    }

    public static Double[] add(Object[] x, Object[] y) {
        return calculate(x, y, new OperatorFunction<Double, Double>() {
            @Override
            public Double apply(Double a, Double b) {
                return a + b;
            }
        });
    }

    public static Double[][] add(Object x, Object[][] y) {
        Object[][] xWrapper = new Object[][] { { x } };
        return calculate(xWrapper, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return add(a, b);
            }
        });
    }

    public static Double[][] add(Object[][] x, Object y) {
        Object[][] yWrapper = new Object[][] { { y } };
        return calculate(x, yWrapper, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return add(a, b);
            }
        });
    }

    public static Object add(Object x, Object y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return add(a, b);
            }
        });
    }

    public static Double[][] subtract(Object[][] x, Object[][] y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return subtract(a, b);
            }
        });
    }

    public static Double[] subtract(Object[] x, Object[] y) {
        return calculate(x, y, new OperatorFunction<Double, Double>() {
            @Override
            public Double apply(Double a, Double b) {
                return a - b;
            }
        });
    }

    public static Double[][] subtract(Object x, Object[][] y) {
        Object[][] xWrapper = new Object[][] { { x } };
        return calculate(xWrapper, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return subtract(a, b);
            }
        });
    }

    public static Double[][] subtract(Object[][] x, Object y) {
        Object[][] yWrapper = new Object[][] { { y } };
        return calculate(x, yWrapper, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return subtract(a, b);
            }
        });
    }

    public static Object subtract(Object x, Object y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return subtract(a, b);
            }
        });
    }

    public static Double[][] multiply(Object[][] x, Object[][] y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return multiply(a, b);
            }
        });
    }

    public static Double[] multiply(Object[] x, Object[] y) {
        return calculate(x, y, new OperatorFunction<Double, Double>() {
            @Override
            public Double apply(Double a, Double b) {
                return a * b;
            }
        });
    }

    public static Double[][] multiply(Object x, Object[][] y) {
        Object[][] xWrapper = new Object[][] { { x } };
        return calculate(xWrapper, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return multiply(a, b);
            }
        });
    }

    public static Double[][] multiply(Object[][] x, Object y) {
        Object[][] yWrapper = new Object[][] { { y } };
        return calculate(x, yWrapper, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return multiply(a, b);
            }
        });
    }

    public static Object multiply(Object x, Object y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return multiply(a, b);
            }
        });
    }

    public static Double[][] divide(Object[][] x, Object[][] y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return divide(a, b);
            }
        });
    }

    public static Double[] divide(Object[] x, Object[] y) {
        return calculate(x, y, new OperatorFunction<Double, Double>() {
            @Override
            public Double apply(Double a, Double b) {
                return a / b;
            }
        });
    }

    public static Double[][] divide(Object x, Object[][] y) {
        Object[][] xWrapper = new Object[][] { { x } };
        return calculate(xWrapper, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return divide(a, b);
            }
        });
    }

    public static Double[][] divide(Object[][] x, Object y) {
        Object[][] yWrapper = new Object[][] { { y } };
        return calculate(x, yWrapper, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return divide(a, b);
            }
        });
    }

    public static Object divide(Object x, Object y) {
        return calculate(x, y, new OperatorFunction<Double[], Object[]>() {
            @Override
            public Double[] apply(Object[] a, Object[] b) {
                return divide(a, b);
            }
        });
    }

    private static Object calculate(Object a, Object b, OperatorFunction<Double[], Object[]> operatorFunction) {
        if (a == null) {
            a = 0d;
        }
        if (b == null) {
            b = 0d;
        }

        if (a.getClass().isArray() || b.getClass().isArray()) {
            boolean aIsTwoDimensional = isTwoDimensional(a);
            boolean bIsTwoDimensional = isTwoDimensional(b);

            if (aIsTwoDimensional || bIsTwoDimensional) {
                Object[][] aValues;
                Object[][] bValues;
                if (a.getClass().isArray()) {
                    Object[] aArray = (Object[]) a;
                    aValues = aIsTwoDimensional ? (Object[][]) aArray : new Object[][] { aArray };
                } else {
                    aValues = new Object[][] { { a } };
                }
                if (b.getClass().isArray()) {
                    Object[] bArray = (Object[]) b;
                    bValues = bIsTwoDimensional ? (Object[][]) bArray : new Object[][] { bArray };
                } else {
                    bValues = new Object[][] { { b } };
                }
                return calculate(aValues, bValues, operatorFunction);
            } else {
                Object[] aArray = a.getClass().isArray() ? (Object[]) a : new Object[] { a };
                Object[] bArray = b.getClass().isArray() ? (Object[]) b : new Object[] { b };
                return operatorFunction.apply(aArray, bArray);
            }
        } else {
            Double aValue = HelperFunctions.toDouble(a);
            Double bValue = HelperFunctions.toDouble(b);

            Double[] apply = operatorFunction.apply(new Double[] { aValue }, new Double[] { bValue });
            return apply[0];
        }
    }

    private static Double[] calculate(Object[] x, Object[] y, OperatorFunction<Double, Double> operatorFunction) {
        if (x == null || y == null || x.length == 0 || y.length == 0) {
            return null;
        }

        int length = Math.max(x.length, y.length);

        Double[] result = new Double[length];
        for (int i = 0; i < length; i++) {
            Double xElem;
            Double yElem;
            if (i >= x.length) {
                if (x.length > 1) {
                    break;
                }
                xElem = HelperFunctions.toDouble(x[0]);
                yElem = HelperFunctions.toDouble(y[i]);
            } else if (i >= y.length) {
                if (y.length > 1) {
                    break;
                }
                xElem = HelperFunctions.toDouble(x[i]);
                yElem = HelperFunctions.toDouble(y[0]);
            } else {
                xElem = HelperFunctions.toDouble(x[i]);
                yElem = HelperFunctions.toDouble(y[i]);
            }
            result[i] = xElem == null || yElem == null ? null : operatorFunction.apply(xElem, yElem);
        }

        return result;
    }

    private static Double[][] calculate(Object[][] x,
            Object[][] y,
            OperatorFunction<Double[], Object[]> operatorFunction) {
        if (x == null || y == null || x.length == 0 || y.length == 0) {
            return null;
        }

        int length = Math.max(x.length, y.length);

        Double[][] result = new Double[length][];
        for (int i = 0; i < length; i++) {
            if (i >= x.length) {
                if (x.length > 1) {
                    break;
                }
                Double[] row = operatorFunction.apply(x[0], y[i]);
                result[i] = row == null ? new Double[] {} : row;
            } else if (i >= y.length) {
                if (y.length > 1) {
                    break;
                }
                Double[] row = operatorFunction.apply(x[i], y[0]);
                result[i] = row == null ? new Double[] {} : row;
            } else {
                Double[] row = operatorFunction.apply(x[i], y[i]);
                result[i] = row == null ? new Double[] {} : row;
            }
        }

        return result;
    }

    private static boolean isTwoDimensional(Object object) {
        if (object == null || !object.getClass().isArray()) {
            return false;
        }

        Object[] array = (Object[]) object;
        for (Object o : array) {
            if (o != null) {
                return o.getClass().isArray();
            }
        }

        return false;
    }

    private interface OperatorFunction<R, P> {
        R apply(P a, P b);
    }
}
