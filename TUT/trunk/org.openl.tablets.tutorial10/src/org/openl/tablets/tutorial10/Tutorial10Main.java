/**
 * OpenL Tablets,  2009
 * https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial10;

import static java.lang.System.out;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.runtime.RuleEngineFactory;

/**
 * Tutorial 10. Example of Spreadsheet tables.
 * <p>
 * Run this class "as Java Application".
 */

public class Tutorial10Main {
    public static void main(String[] args) {
        out.println();
        out.println("* OpenL Tutorial 10\n");

        out.println("Getting wrapper...\n");
        RuleEngineFactory<Tutorial10Rules> rulesFactory = new RuleEngineFactory<Tutorial10Rules>("rules/Tutorial_10.xlsx", Tutorial10Rules.class);
        Tutorial10Rules rules = rulesFactory.newInstance();

        Car car = new Car(CarBrand.BMW, "Z4 sDrive30i");
        Address address = new Address(Country.Belarus, "Minsk");
        out.println("* Executing OpenL rules...\n");
        out.println("Get Price for order: ");
        out.println(rules.getPriceForOrder(car, 4, address));

    }
}
