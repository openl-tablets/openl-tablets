/**
 * Created Apr 4, 2007
 */
package org.openl.rules.dt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.rules.testmethod.TestResult;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;

/**
 * @author snshor
 *
 */
public class DTRuleQuery {

    static class DTParamNameSelector implements IDTSelector {

        String paramName;

        public DTParamNameSelector(String name) {
            paramName = name;
        }

        public boolean selectDT(DecisionTable dt) {
            if (paramName == null) {
                return true;
            }

            return selectParametrs(dt, paramName).length > 0;
        }

        public DTParameterInfo[] selectParametrs(DecisionTable dt, String paramName) {

            List<DTParameterInfo> list = new ArrayList<DTParameterInfo>();
            IDTCondition[] cc = dt.getConditionRows();
            for (int i = 0; i < cc.length; i++) {
                IParameterDeclaration[] params = cc[i].getParams();
                for (int j = 0; j < params.length; j++) {
                    if (paramName == null || params[j].getName().equals(paramName)) {
                        list.add(cc[i].getParameterInfo(j));
                    }
                }
            }

            // TODO actions

            return list.toArray(new DTParameterInfo[0]);
        }

    }

    static class DTRuleParamSelector implements IDTRuleSelector {
        String paramName;
        Object paramValue;
        ValueComparator comparator;

        /**
         * @param paramName2
         * @param paramValue2
         * @param cmp
         */
        public DTRuleParamSelector(String paramName, Object paramValue, ValueComparator cmp) {
            this.paramName = paramName;
            this.paramValue = paramValue;
            comparator = cmp;
            if (comparator == null) {
                comparator = new ValueComparator();
            }
        }

        public boolean selectDTRule(DTRule rule) {

            DTParameterInfo[] pi = new DTParamNameSelector(paramName).selectParametrs(rule.getDecisionTable(),
                    paramName);

            for (int i = 0; i < pi.length; i++) {
                Object value = pi[i].getValue(rule.getRuleRow());

                if (comparator.compareParams(value, paramValue)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static interface IDTRuleSelector {
        boolean selectDTRule(DTRule rule);
    }

    public static interface IDTSelector {
        boolean selectDT(DecisionTable dt);
    }

    static public class ValueComparator {
        boolean selectBlanks = true;
        boolean checkArrays = true;

        public boolean compareParams(Object param, Object test) {
            if (param == null) {
                return selectBlanks;
            }

            if (param.getClass().isArray() && checkArrays) {
                int len = Array.getLength(param);
                for (int i = 0; i < len; i++) {
                    Object px = Array.get(param, i);
                    if (TestResult.compareResult(px, test)) {
                        return true;
                    }
                }
            }

            return TestResult.compareResult(param, test);
        }
    }

    static public DTRule[] select(IOpenClass ioc, IDTRuleSelector rsel, IDTSelector dtsel) {
        ArrayList<DTRule> list = new ArrayList<DTRule>(100);

        for (Iterator<IOpenMethod> iter = ioc.methods(); iter.hasNext();) {
            IOpenMethod m = iter.next();

            if (m instanceof DecisionTable) {
                DecisionTable dt = (DecisionTable) m;

                if (dtsel != null && !dtsel.selectDT(dt)) {
                    continue;
                }

                int n = dt.getNumberOfRules();

                for (int i = 0; i < n; i++) {
                    DTRule rule = new DTRule(dt, i);

                    if (rsel != null && !rsel.selectDTRule(rule)) {
                        continue;
                    }
                    list.add(rule);

                }

            }
        }

        return list.toArray(new DTRule[0]);
    }

    static public DTRule[] selectRulesWithParam(IOpenClass ioc, String paramName, Object paramValue, ValueComparator cmp) {
        IDTSelector dtsel = new DTParamNameSelector(paramName);
        IDTRuleSelector rsel = new DTRuleParamSelector(paramName, paramValue, cmp);

        return select(ioc, rsel, dtsel);

    }

}
