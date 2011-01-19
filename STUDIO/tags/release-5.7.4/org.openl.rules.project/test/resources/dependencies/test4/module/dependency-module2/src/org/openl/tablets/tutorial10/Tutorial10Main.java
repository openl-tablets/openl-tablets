/**
 * OpenL Tablets,  2009
 * https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial10;

import static java.lang.System.out;

import java.util.Calendar;

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.tablets.tutorial10.domain.Address;
import org.openl.tablets.tutorial10.domain.Car;
import org.openl.tablets.tutorial10.domain.CarBrand;
import org.openl.tablets.tutorial10.domain.Country;
import org.openl.vm.IRuntimeEnv;

/**
 * Tutorial 10. Example of Spreadsheet tables.
 * <p>
 * Run this class "as Java Application".
 */

public class Tutorial10Main {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        out.println();
        out.println("* OpenL Tutorial 10\n");

        out.println("Getting wrapper...\n");
        RuleEngineFactory<Tutorial10Rules> rulesFactory = new RuleEngineFactory<Tutorial10Rules>("rules/Tutorial_10.xlsx", Tutorial10Rules.class);
        Tutorial10Rules rules = rulesFactory.makeInstance();
        
        // We should setup runtime context with proper values
        // These values will be used to dispatch call to appropriate rule
       
        // Getting runtime environment which contains context 
        IRuntimeEnv env = ((IEngineWrapper) rules).getRuntimeEnv();

        // Creating context (most probably in future, the code will be different)
        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        env.setContext(context);

        // Creating current date value
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);

        // Setting current date in context, which will be used in dispatch
        context.setCurrentDate(calendar.getTime());
        
        // Preparing data for rules call
        Car car = new Car(CarBrand.BMW, "Z4 sDrive30i");
        Address address = new Address(Country.Belarus, "Minsk");
        
        out.println("* Executing OpenL rules...\n");
        out.println("Get Price for order: ");
        out.println(rules.getPriceForOrder(car, 4, address));

    }
}
