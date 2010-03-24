/**
 * Created Apr 3, 2007
 */
package com.exigen.rules.openl.integrator;

import org.openl.rules.dt.DTRule;
import org.openl.rules.dt.DecisionTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;

/**
 * @author snshor
 * 
 */
public class RulePrinter {

    public void printRule(IOpenClass ioc, String name, int i) {
        IOpenMethod om = AOpenClass.getSingleMethod(name, ioc.methods());
        DecisionTable dt = (DecisionTable) om;

        printDTRule(dt, i);
    }

    public void printDTRule(DTRule rule) {
        System.out.println(rule.display(new StringBuffer(), "HTML"));
    }

    private void printDTRule(DecisionTable dt, int ruleRow) {

        DTRule rule = new DTRule(dt, ruleRow);
        System.out.println(rule.display(new StringBuffer(), "HTML"));
    }
}