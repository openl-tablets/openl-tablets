/*
 * Created on May 15, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.print;

import java.text.NumberFormat;

/**
 * @author snshor
 */
public class DoublePrinter {

    public static void main(String[] args) {
        System.out.println(printDouble(10, 3, 0));
        System.out.println(printDouble(10, 3, 3));

        System.out.println(printDouble(10));
        System.out.println(printDouble(10. / 3));
        System.out.println(printDouble(10. / 30));
        System.out.println(printDouble(10. / 300));
        System.out.println(printDouble(10. / 3000));
        System.out.println(printDouble(10. / 30000));
        System.out.println(printDouble(0));
        System.out.println(printDouble(12345.6789));

    }

    static String printDouble(double dd) {
        double d = dd < 0 ? -dd : dd;

        double x = 1;

        for (int i = 0; i < 7; i++) {
            if (d > x) {
                return printDouble(dd, 2 + i, 0);
            }
            x /= 10;
        }

        return String.valueOf(dd);

    }

    static String printDouble(double d, int maxDigits, int minDigits) {

        // System.out.println("" + d + " : " + minDigits + "," + maxDigits);
        NumberFormat nf = NumberFormat.getNumberInstance();

        nf.setMinimumFractionDigits(minDigits);
        nf.setMaximumFractionDigits(maxDigits);

        return nf.format(d);
    }

}
