package org.openl.rules.validator.dt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.OpenLRuntimeException;
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
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl.CDecisionTableImpl;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl;
import com.exigen.ie.constrainer.consistencyChecking.Overlapping;
import com.exigen.ie.constrainer.consistencyChecking.Uncovered;

public class ValidationAlgorithm {
    
    private IDTValidatedObject dtValidatedObject;

    private Constrainer constrainer = new Constrainer("Validation");
    private IDTCondition[] dtConditions;
    private IOpenMethod[] condMethods;
    private IntExpArray vars;
    private OpenL openl;    

    public ValidationAlgorithm(IDTValidatedObject dtValidatedObject, OpenL openl) {
        this.dtValidatedObject = dtValidatedObject;
        this.dtConditions = dtValidatedObject.getDT().getConditionRows();
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
    
    private IOpenMethod makeConditionMethod(IDTCondition condition, DTAnalyzer dtAnalyzer) {        
        IMethodSignature newSignature = getNewSignature(condition, dtAnalyzer);
        IOpenClass methodType = JavaOpenClass.getOpenClass(IntBoolExp.class);
        IOpenClass declaringClass = dtAnalyzer.getDt().getDeclaringClass();
        String conditionName = condition.getName();

        OpenMethodHeader methodHeader = new OpenMethodHeader(conditionName, methodType, newSignature, declaringClass);

        IBindingContext cxt = new ModuleBindingContext(openl.getBinder().makeBindingContext(), 
                (ModuleOpenClass) declaringClass);
        
        IOpenSourceCodeModule formulaSourceCode = condition.getConditionEvaluator().getFormalSourceCode(condition);
        return OpenLManager.makeMethod(openl, formulaSourceCode, methodHeader, cxt);
    }

    private IMethodSignature getNewSignature(IDTCondition condition, DTAnalyzer dtAnalyzer) {
        IParameterDeclaration[] paramDeclarations = condition.getParams();

        IParameterDeclaration[] referencedSignatureParams = dtAnalyzer.referencedSignatureParams(condition);
                
        return makeNewSignature(paramDeclarations, referencedSignatureParams, dtAnalyzer);
    }
    
    private IntBoolExp makeExpression(int rule, int cnum, DTAnalyzer dtAnalyzer) {
        IDTCondition cond = dtConditions[cnum];

        Object[] values = cond.getParamValues()[rule];

        if (values == null) {
            return new IntBoolExpConst(constrainer, true);
        }

        int nargs = condMethods[cnum].getSignature().getNumberOfArguments();

        // /make params from vars and values

        Object[] args = new Object[nargs];

        // int ndtArgs = dtvo.getDT().getSignature().getNumberOfArguments();
        int ndtArgs = nargs - values.length;

        for (int i = 0; i < nargs; i++) {
            String name = condMethods[cnum].getSignature().getParameterName(i);
            if (i < ndtArgs) {
                args[i] = findVar(vars, name);
            } else {
                args[i] = transformValue(name, dtConditions[cnum],
                        values[i - ndtArgs], dtAnalyzer);
            }
        }
        Object instance = getInstance();
        return (IntBoolExp) condMethods[cnum].invoke(instance, args, openl
                .getVm().getRuntimeEnv());
    }
    
    private IntBoolExp[][] makeExpressions(DTAnalyzer dtAnalyzer) {
        int numRules = dtValidatedObject.getDT().getNumberOfRules();
        IntBoolExp[][] ary = new IntBoolExp[numRules][condMethods.length];

        for (int i = 0; i < numRules; i++) {
            IntBoolExp[] ruleExp = new IntBoolExp[condMethods.length];
            ary[i] = ruleExp;

            for (int j = 0; j < condMethods.length; j++) {
                ruleExp[j] = makeExpression(i, j, dtAnalyzer);
            }
        }
        return ary;
    }

    private IMethodSignature makeNewSignature(IParameterDeclaration[] paramDeclarations,
            IParameterDeclaration[] referencedSignatureParams, DTAnalyzer dtan) {
        
        List<IParameterDeclaration> parameters = new ArrayList<IParameterDeclaration>();
        
        for (IParameterDeclaration paramDecl : referencedSignatureParams) {
            IOpenClass newType = dtan.transformSignatureType(paramDecl, dtValidatedObject);

            if (newType == null) {
                newType = paramDecl.getType();
            }
            parameters.add(new ParameterDeclaration(newType, paramDecl.getName(),
                    paramDecl.getDirection()));
        }

        for (IParameterDeclaration paramDecl : paramDeclarations) {
            IOpenClass newType = dtValidatedObject.getTransformer()
                    .transformParameterType(paramDecl);
            
            if (newType == null) {
                parameters.add(paramDecl);
            } else {
                parameters.add(new ParameterDeclaration(newType, paramDecl.getName(),
                        paramDecl.getDirection()));
            }
        }        
        return new MethodSignature(parameters.toArray(new IParameterDeclaration[parameters.size()]));
    }
    
    private IntExpArray makeVars(DTAnalyzer dtAnalyzer) {
        List<IntExp> vars = new ArrayList<IntExp>();
        
        for (Iterator<DTParamDescription> iterator = dtAnalyzer.dtparams(); iterator.hasNext();) {
            DTParamDescription dtParamDescr = iterator.next();
            String varName = dtParamDescr.getOriginalDeclaration().getName();            
            IOpenClass varType = dtParamDescr.getOriginalDeclaration().getType();
            
            IntVar var = dtValidatedObject.getTransformer().makeSignatureVar(varName, varType, constrainer);
            
            if (var != null) {
                vars.add(var);
            } else {
                throw new OpenLRuntimeException(String.format("Could not create domain for %s", varName));
            }
        }
        IntExpArray iary = new IntExpArray(constrainer, vars);
        return iary;
    }
    
    private Object transformValue(String name, IDTCondition condition,
            Object value, DTAnalyzer dtan) {
        return dtValidatedObject.getTransformer().transformParameterValue(name,
                condition, value, constrainer, dtan);
    }

    public DTValidationResult validateDT() {
        DecisionTable decisionTable = dtValidatedObject.getDT();
        
        IConditionSelector conditionSelector = dtValidatedObject.getSelector();

        if (conditionSelector != null) {
            dtConditions = OpenIterator.fromArray(dtConditions).select(conditionSelector).asList().toArray(
                    new IDTCondition[0]);
        }

        if (dtConditions.length == 0) {
            return new DTValidationResult(decisionTable, new DTOverlapping[0],
                    new DTUncovered[0]);
        }

        DTAnalyzer dtAnalyzer = new DTAnalyzer(decisionTable);

        for (IDTCondition dtCondition :dtConditions) {
            if (dtAnalyzer.containsFormula(dtCondition)) {
                return new DTValidationResult(decisionTable, new DTOverlapping[0],
                        new DTUncovered[0]);
            }
        }

        condMethods = new IOpenMethod[dtConditions.length];

        for (int i = 0; i < dtConditions.length; i++) {
            condMethods[i] = makeConditionMethod(dtConditions[i], dtAnalyzer);
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
