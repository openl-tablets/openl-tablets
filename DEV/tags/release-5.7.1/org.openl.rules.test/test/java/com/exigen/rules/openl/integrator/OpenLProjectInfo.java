package com.exigen.rules.openl.integrator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.element.Action;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.DecisionTableParameterInfo;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class OpenLProjectInfo {

    private HashMap<Object, Object> decisionTables = new HashMap<Object, Object>();
    private HashSet<Object> types = new HashSet<Object>();

    public void load(IOpenClass ioc) {
        loadDecisionTables(ioc);
    }

    private void loadDecisionTables(IOpenClass ioc) {

        for (Iterator<IOpenMethod> iter = ioc.methods(); iter.hasNext();) {
            
            IOpenMethod m = iter.next();

            if (m instanceof DecisionTable) {
//                System.out.println(m.getName());
                loadDT((DecisionTable) m);
            }
        }
    }

    private void loadDT(DecisionTable table) {

        IOpenClass type = table.getType();

        if (type != JavaOpenClass.VOID) {
//            System.out.println("WARNING: Only Decision Tables with type 'void' can be converted");
            return;
        }

        IMethodSignature signature = table.getSignature();
        IOpenClass[] pTypes = signature.getParameterTypes();

        for (int i = 0; i < pTypes.length; i++) {

            if (pTypes[i] instanceof JavaOpenClass) {
//                System.out.print("  " + pTypes[i].getName());
//                System.out.println(" " + signature.getParameterName(i));
                types.add(pTypes[i]);

            } else {
//                System.out.println("WARNING: Type " + pTypes[i].getName() + " is not a Java Class");
            }
        }

        if (!loadConditions(table)) {
            return;
        }

        if (!loadActions(table)) {
            return;
        }

        decisionTables.put(table.getHeader().getName(), table);
    }

    private boolean loadConditions(DecisionTable dt) {

        ICondition[] cc = dt.getConditionRows();

        for (int i = 0; i < cc.length; i++) {
            if (!(cc[i] instanceof Condition)) {
//                System.out.println("WARNING: Condition is not a DTCondition: " + cc[i].getName());
                return false;
            }
            if (!loadConditionOrAction((Condition) cc[i]))
                return false;
        }

        return true;
    }

    private boolean loadActions(DecisionTable dt) {

        IAction[] cc = dt.getActionRows();

        for (int i = 0; i < cc.length; i++) {
            if (!(cc[i] instanceof Action)) {
//                System.out.println("WARNING: Action is not a DTAction: " + cc[i].getName());
                return false;
            }

            if (!loadConditionOrAction((Action) cc[i]))
                return false;
        }

        return true;
    }

    private boolean loadConditionOrAction(FunctionalRow ca) {

        //System.out.print("     " + ca.getName());

        CompositeMethod cm = (CompositeMethod) ca.getMethod();

        IParameterDeclaration[] params = ca.getParams();

        for (int i = 0; i < params.length; i++) {

            if (!(params[i].getType() instanceof JavaOpenClass)) {
                System.out.println("Type " + params[i].getType().getName());
            }

//            System.out.print(i == 0 ? "(" : ", ");
//            System.out.print(params[i].getType().getInstanceClass().getName());
//            System.out.print(" " + params[i].getName());
//            System.out.print(" : " + ca.getParamPresentation()[i]);
        }

//        System.out.println(")");
//        System.out.println("     " + cm.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode());

        Object[][] paramValues = ca.getParamValues();

        int len = paramValues.length;

        for (int j = 0; j < len; ++j) {
            for (int i = 0; i < params.length; i++) {

                // Object[] paramRow = paramValues[i];
                DecisionTableParameterInfo pi = ca.getParameterInfo(i);

//                System.out.print("   " + pi.getValue(j));
            }
//            System.out.println();
        }

        return true;
    }

    public IParameterDeclaration[] getParameters(IOpenMethod m) {

        IMethodSignature signature = m.getSignature();
        IOpenClass[] pTypes = signature.getParameterTypes();

        ParameterDeclaration[] pd = new ParameterDeclaration[pTypes.length];

        for (int i = 0; i < pTypes.length; i++) {
            pd[i] = new ParameterDeclaration(pTypes[i], signature.getParameterName(i));
        }

        return pd;
    }

    public DecisionTable getDecisionTable(String name) throws Exception {

        DecisionTable dt = (DecisionTable) decisionTables.get(name);

        if (dt == null)
            throw new Exception("DecisionTable " + name + " not found");

        return dt;
    }

    public Iterator<Object> allTables() {
        return decisionTables.values().iterator();
    }

    public int getNumberOfRules(DecisionTable table) {

        if (table.getConditionRows().length > 0)
            return getParamValues(table.getConditionRows()[0]).length;

        if (table.getActionRows().length > 0)
            return getParamValues(table.getActionRows()[0]).length;

        return 0;
    }

    public Object[][] getParamValues(IDecisionRow row) {
        return ((FunctionalRow) row).getParamValues();
    }
}
