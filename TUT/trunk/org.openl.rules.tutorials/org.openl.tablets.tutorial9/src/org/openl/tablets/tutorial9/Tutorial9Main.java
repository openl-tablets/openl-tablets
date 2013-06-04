/**
 * OpenL Tablets,  2009
 * https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial9;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * Tutorial 9. Example of Spreadsheet tables.
 * <p>
 * Run this class "as Java Application".
 */

public class Tutorial9Main {
    public static void main(String[] args) {
        java.lang.System.out.println();
        java.lang.System.out.println("* OpenL Tutorial 9\n");

        java.lang.System.out.println("Getting interface...");
        Tutorial_9RulesInterface tutorial9 = new RulesEngineFactory<Tutorial_9RulesInterface>(
                Tutorial_9RulesInterface.__src, Tutorial_9RulesInterface.class).newEngineInstance();

        java.lang.System.out.println("* Executing OpenL tables...\n");
        java.lang.System.out.println("totalAssets():");
        SpreadsheetResult result = tutorial9.totalAssets();

        FinePrint fine = new FinePrint(result);
        fine.print();

        java.lang.System.out.println("incomeForecast(double bonusRate, double sharePrice):");
        result = tutorial9.incomeForecast(0.15, 15.0);
        new FinePrint(result).print();

        java.lang.System.out.println("Lower bonusRate but higher sharePrice...");
        result = tutorial9.incomeForecast(0.05, 35.0);
        new FinePrint(result).print();
    }

    /**
     * Helper class to print out SpreadsheetResult in a neat way.
     */
    private static class FinePrint {
        private final int height;
        private final int width;
        private final String[][] cells;
        private final int[] maxL;

        public FinePrint(SpreadsheetResult result) {
            this(result.height() + 1, result.width() + 1);
            for (int x = 0; x < result.width(); x++) {
                set(0, x + 1, result.getColumnName(x));
            }

            for (int y = 0; y < result.height(); y++) {
                set(y + 1, 0, result.getRowName(y));

                for (int x = 0; x < result.width(); x++) {
                    set(y + 1, x + 1, result.getValue(y, x));
                }
            }
        }

        public FinePrint(int height, int width) {
            this.height = height;
            this.width = width;
            cells = new String[height][width];
            maxL = new int[width];
        }

        public void set(int y, int x, Object value) {
            if (value == null) {
                cells[y][x] = null;
            } else {
                cells[y][x] = value.toString();
            }
        }

        public void print() {
            initMaxL();

            for (int y = 0; y < height; y++) {
                java.lang.System.out.print("\t");
                for (int x = 0; x < width; x++) {
                    if (x > 0) {
                        java.lang.System.out.print(" | ");
                    }

                    String s = cells[y][x];
                    if (s == null)
                        s = "";
                    java.lang.System.out.print(s);

                    int left = maxL[x] - s.length();
                    for (int i = 0; i < left; i++) {
                        java.lang.System.out.print(' ');
                    }
                }
                java.lang.System.out.println();
            }
            java.lang.System.out.println();
        }

        private void initMaxL() {
            for (int x = 0; x < width; x++) {
                int max = 0;
                for (int y = 0; y < height; y++) {
                    String s = cells[y][x];
                    if (s != null) {
                        max = Math.max(max, s.length());
                    }
                }
                maxL[x] = max;
            }
        }
    }
}
