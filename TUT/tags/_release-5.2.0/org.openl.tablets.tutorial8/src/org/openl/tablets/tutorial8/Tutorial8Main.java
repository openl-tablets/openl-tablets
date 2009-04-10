/**
 * OpenL Tablets,  2009
 * https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial8;

import static java.lang.System.out;

/**
 * Tutorial 8. Example of TBasic tables.
 * <p>
 * Run this class "as Java Application".
 */

public class Tutorial8Main {
    public static void main(String[] args) {
        out.println();
        out.println("* OpenL Tutorial 8\n");

        out.println("Getting wrapper...");
        Tutorial_8Wrapper tutorial8 = new Tutorial_8Wrapper();

        out.println("* Executing OpenL tables...\n");
        out.println("factorial(int):");
        for (int i = 0; i <= 10; i++) {
            out.printf("  %2d: %d\n", i, tutorial8.factorial(i));
        }
        out.println();

        Loan loan = new Loan();
        loan.setPurpose("Car");
        loan.setAmount(25000);
        loan.setRate(0.04);
        loan.setYears(5);

        out.println("listPayments(Loan):");
        out.printf("  %s\n", loan);
        Payments payments = tutorial8.listPayments(loan);
        for (int i = 0; i < payments.getYears(); i++) {
            double fixed = payments.getAmount(i);
            double commission = payments.getCommission(i);
            double toPay = payments.getToPay(i);
            out.printf("  %d: %.2f (%.2f  + %.2f)\n", i, toPay, fixed, commission);
        }
        out.println();

        out.println("totalPayments(Payments):");
        out.printf("  %.2f\n", tutorial8.totalPayments(payments));
        out.println();
    }
}
