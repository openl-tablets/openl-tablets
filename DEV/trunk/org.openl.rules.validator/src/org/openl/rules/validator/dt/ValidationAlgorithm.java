package org.openl.rules.validator.dt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenIterator;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntBoolExpConst;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl;
import com.exigen.ie.constrainer.consistencyChecking.Overlapping;
import com.exigen.ie.constrainer.consistencyChecking.Uncovered;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl.CDecisionTableImpl;

public class ValidationAlgorithm {
    
    private IDTValidatedObject dtValidatedObject;

    private Constrainer constrainer = new Constrainer("Validation");
    private IDTCondition[] dtCondition;
    private IOpenMethod[] cmethods;
    private IntExpArray vars;
    private OpenL openl;    

    public ValidationAlgorithm(IDTValidatedObject dtValidatedObject, OpenL openl) {
        this.dtValidatedObject = dtValidatedObject;
        this.openl = openl;
    }
    
    private Object findVar(IntExpArray vars2, String name) {
        for (int i = 0; i < vars2.size(); i++) {
            if (name.equals(vars2.elementAt(i).name())) {
                return vars2.elementAt(i);
            }
        }

        return null;
    }
    
    private Object getInstance() {
        // TODO Auto-generated method stub
        return null;
    }
    
    private IOpenMethod makeCMethod(IDTCondition condition, DTAnalyzer dtan) {

        // IOpenSourceCodeModule src = condition.getSourceCodeModule();

        // DecisionTable dt = dtan.getDt();
        IOpenSourceCodeModule src = condition.getConditionEvaluator()
                .getFormalSourceCode(condition);

        IParameterDeclaration[] pd = condition.getParams();

        IParameterDeclaration[] dtpd = dtan
                .referencedSignatureParams(condition);
        // IMethodSignature dtsignature = dt.getSignature();

        IMethodSignature newSignature = makeNewSignature(pd, dtpd, dtan);

        IOpenClass methodType = JavaOpenClass
                .getOpenClass(IntBoolExp.class);

        IOpenClass declaringClass = dtan.getDt().getDeclaringClass();

        OpenMethodHeader methodHeader = new OpenMethodHeader(condition
                .getName(), methodType, newSignature, declaringClass);

        IBindingContext cxt = new ModuleBindingContext(openl.getBinder()
                .makeBindingContext(), (ModuleOpenClass) declaringClass);
        return OpenLManager.makeMethod(openl, src, methodHeader, cxt);

    }
    
    private IntBoolExp makeExpression(int rule, int cnum, DTAnalyzer dtan) {
        IDTCondition cond = dtCondition[cnum];

        Object[] values = cond.getParamValues()[rule];

        if (values == null) {
            return new IntBoolExpConst(constrainer, true);
        }

        int nargs = cmethods[cnum].getSignature().getNumberOfArguments();

        // /make params from vars and values

        Object[] args = new Object[nargs];

        // int ndtArgs = dtvo.getDT().getSignature().getNumberOfArguments();
        int ndtArgs = nargs - values.length;

        for (int i = 0; i < nargs; i++) {
            String name = cmethods[cnum].getSignature().getParameterName(i);
            if (i < ndtArgs) {
                args[i] = findVar(vars, name);
            } else {
                args[i] = transformValue(name, dtCondition[cnum],
                        values[i - ndtArgs], dtan);
            }

        }

        Object instance = getInstance();
        return (IntBoolExp) cmethods[cnum].invoke(instance, args, openl
                .getVm().getRuntimeEnv());
    }
    
    private IntBoolExp[][] makeExpressions(DTAnalyzer dtan) {
        int nrules = dtValidatedObject.getDT().getNumberOfRules();
        IntBoolExp[][] ary = new IntBoolExp[nrules][cmethods.length];

        for (int i = 0; i < nrules; i++) {
            IntBoolExp[] ruleExp = new IntBoolExp[cmethods.length];
            ary[i] = ruleExp;

            for (int j = 0; j < cmethods.length; j++) {
                ruleExp[j] = makeExpression(i, j, dtan);
            }

        }

        return ary;
    }

    private IMethodSignature makeNewSignature(IParameterDeclaration[] pd,
            IParameterDeclaration[] dtpd, DTAnalyzer dtan) {

        IParameterDeclaration[] pdd = new ParameterDeclaration[dtpd.length
                + pd.length];

        for (int i = 0; i < dtpd.length; i++) {
            IOpenClass newType = dtan.transformSignatureType(dtpd[i], dtValidatedObject);

            // IOpenClass newType = dtvo.getTransformer()
            // .transformSignatureType(dtpd[i]);
            if (newType == null) {
                newType = dtpd[i].getType();
            }
            pdd[i] = new ParameterDeclaration(newType, dtpd[i].getName(),
                    dtpd[i].getDirection());
        }

        for (int i = 0; i < pd.length; i++) {
            IOpenClass newType = dtValidatedObject.getTransformer()
                    .transformParameterType(pd[i]);
            pdd[i + dtpd.length] = newType == null ? pd[i]
                    : new ParameterDeclaration(newType, pd[i].getName(),
                            pd[i].getDirection());
        }

        return new MethodSignature(pdd);
    }
    
    private IntExpArray makeVars(DTAnalyzer dtan) {

        List<IntExp> v = new ArrayList<IntExp>();

        // IMethodSignature dtsign = dtvo.getDT().getSignature();

        // int nargs = dtan.getNumberOfDTSignatureParams();

        for (Iterator<DTParamDescription> iterator = dtan.dtparams(); iterator
                .hasNext();) {
            DTParamDescription dtp = iterator.next();

            String vname = dtp.getOriginalDeclaration().getName();

            IntVar var = dtValidatedObject.getTransformer().makeSignatureVar(vname,
                    dtp.getOriginalDeclaration().getType(), constrainer);
            if (var != null) {
                v.add(var);
            } else {
                throw new RuntimeException("Could not create domain for "
                        + vname);
            }
        }

        IntExpArray iary = new IntExpArray(constrainer, v);
        return iary;
    }
    
    private Object transformValue(String name, IDTCondition condition,
            Object value, DTAnalyzer dtan) {
        return dtValidatedObject.getTransformer().transformParameterValue(name,
                condition, value, constrainer, dtan);
    }

    public DTValidationResult validateDT() {
        DecisionTable dt = dtValidatedObject.getDT();

        dtCondition = dt.getConditionRows();

        IConditionSelector conditionSelector = dtValidatedObject.getSelector();

        if (conditionSelector != null) {
            dtCondition = OpenIterator.fromArray(dtCondition).select(conditionSelector).asList().toArray(
                    new IDTCondition[0]);
        }

        if (dtCondition.length == 0) {
            return new DTValidationResult(dt, new DTOverlapping[0],
                    new DTUncovered[0]);
        }

        DTAnalyzer dtAnalyzer = new DTAnalyzer(dt);

        for (int i = 0; i < dtCondition.length; i++) {
            if (dtAnalyzer.containsFormula(dtCondition[i])) {
                return new DTValidationResult(dt, new DTOverlapping[0],
                        new DTUncovered[0]);
            }

        }

        cmethods = new IOpenMethod[dtCondition.length];

        for (int i = 0; i < dtCondition.length; i++) {
            cmethods[i] = makeCMethod(dtCondition[i], dtAnalyzer);
        }

        vars = makeVars(dtAnalyzer);
        IntBoolExp[][] exp = makeExpressions(dtAnalyzer);

        CDecisionTableImpl cdt = new CDecisionTableImpl(exp, vars);
        DTCheckerImpl dtc = new DTCheckerImpl(cdt);

        List<Uncovered> completeness = dtc.checkCompleteness();
        List<Overlapping> overlappings = dtc.checkOverlappings();

        System.out.println("C: " + completeness);
        System.out.println("O:" + overlappings);

        return new DTValidationResult(dtValidatedObject.getDT(), overlappings
                .toArray(new Overlapping[0]), completeness
                .toArray(new Uncovered[0]), dtValidatedObject.getTransformer(), dtAnalyzer);
    }

}
