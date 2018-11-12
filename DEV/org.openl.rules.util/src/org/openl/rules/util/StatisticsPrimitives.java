package org.openl.rules.util;

/**
 * Workaround for Value types.
 */
public class StatisticsPrimitives {


    public static Byte max(byte... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        byte max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }

        return max;
    }

    public static Short max(short... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        short max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }

        return max;
    }

    public static Integer max(int... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        int max = values[0];
        for (int j = 1; j < values.length; j++) {
            if (values[j] > max) {
                max = values[j];
            }
        }

        return max;
    }

    public static Long max(long... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        long max = values[0];
        for (int j = 1; j < values.length; j++) {
            if (values[j] > max) {
                max = values[j];
            }
        }

        return max;
    }

    public static Float max(float... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        float max = values[0];
        for (int j = 1; j < values.length; j++) {
            if (Float.isNaN(values[j])) {
                return Float.NaN;
            }
            if (values[j] > max) {
                max = values[j];
            }
        }

        return max;
    }

    public static Double max(double... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        double max = values[0];
        for (int j = 1; j < values.length; j++) {
            if (Double.isNaN(values[j])) {
                return Double.NaN;
            }
            if (values[j] > max) {
                max = values[j];
            }
        }

        return max;
    }


    public static Byte min(byte... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        byte min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }

        return min;
    }

    public static Short min(short... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        short min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }

        return min;
    }

    public static Integer min(int... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        int min = values[0];
        for (int j = 1; j < values.length; j++) {
            if (values[j] < min) {
                min = values[j];
            }
        }

        return min;
    }

    public static Long min(long... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        long min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }

        return min;
    }

    public static Float min(float... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        float min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (Float.isNaN(values[i])) {
                return Float.NaN;
            }
            if (values[i] < min) {
                min = values[i];
            }
        }

        return min;
    }

    public static Double min(double... values) {
        if (values == null || values.length == 0) {
            return null;
        }

        double min = values[0];
        for (int i = 1; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                return Double.NaN;
            }
            if (values[i] < min) {
                min = values[i];
            }
        }

        return min;
    }

}
