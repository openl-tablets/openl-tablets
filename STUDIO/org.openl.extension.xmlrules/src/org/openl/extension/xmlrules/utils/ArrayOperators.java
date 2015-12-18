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
                xElem = toDouble(x[0]);
                yElem = toDouble(y[i]);
            } else if (i >= y.length) {
                if (y.length > 1) {
                    break;
                }
                xElem = toDouble(x[i]);
                yElem = toDouble(y[0]);
            } else {
                xElem = toDouble(x[i]);
                yElem = toDouble(y[i]);
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

    private static Double toDouble(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Double) {
            return (Double) x;
        }

        if (x instanceof String) {
            return Double.valueOf((String) x);
        }

        // Other number types
        if (x instanceof Number) {
            return ((Number) x).doubleValue();
        }

        throw new IllegalArgumentException("Can't convert to double");
    }

    private interface OperatorFunction<R, P> {
        R apply(P a, P b);
    }
}
