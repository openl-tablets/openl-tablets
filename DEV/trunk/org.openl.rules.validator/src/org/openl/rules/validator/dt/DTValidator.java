/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator.dt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.validator.IValidatedObject;
import org.openl.rules.validator.IValidationResult;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.AOpenClass;
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

/**
 * @author snshor
 *
 */
public class DTValidator implements IDTValidator {

    static class ValidationAlgorithm {
        IDTValidatedObject dtvo;

        Constrainer C = new Constrainer("Validation");
        IDTCondition[] cc;
        IOpenMethod[] cmethods;
        IntExpArray vars;

        // /**
        // * @param pd
        // * @param dtsignature
        // * @return
        // * @deprecated
        // */
        // private IMethodSignature makeNewSignature(IParameterDeclaration[] pd,
        // IMethodSignature dtsignature) {
        // IOpenClass[] dttypes = dtsignature.getParameterTypes();
        //
        // IParameterDeclaration[] pdd = new ParameterDeclaration[dttypes.length
        // + pd.length];
        //
        // for (int i = 0; i < dttypes.length; i++) {
        // IOpenClass newType = dtvo.getTransformer()
        // .transformSignatureType(new ParameterDeclaration( dttypes[i],
        // dtsignature.getParameterName(i),
        // dtsignature.getParameterDirection(i)));
        // if (newType == null)
        // newType = dttypes[i];
        // pdd[i] = new ParameterDeclaration(newType, dtsignature
        // .getParameterName(i), dtsignature
        // .getParameterDirection(i));
        // }
        //
        // for (int i = 0; i < pd.length; i++) {
        // IOpenClass newType = dtvo.getTransformer()
        // .transformParameterType(pd[i]);
        // pdd[i + dttypes.length] = newType == null ? pd[i]
        // : new ParameterDeclaration(newType, pd[i].getName(),
        // pd[i].getDirection());
        // }
        //
        // return new MethodSignature(pdd);
        // }
        OpenL openl;

        public ValidationAlgorithm(IDTValidatedObject dtvo, OpenL openl) {
            this.dtvo = dtvo;
            this.openl = openl;
        }

        /**
         * @param vars2
         * @param name
         * @return
         */
        private Object findVar(IntExpArray vars2, String name) {
            for (int i = 0; i < vars2.size(); i++) {
                if (name.equals(vars2.elementAt(i).name())) {
                    return vars2.elementAt(i);
                }
            }

            return null;
        }

        /**
         * @return
         */
        private Object getInstance() {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * @param condition
         * @param dt
         * @return
         */
        private IOpenMethod makeCMethod(IDTCondition condition, DTAnalyzer dtan) {

            // IOpenSourceCodeModule src = condition.getSourceCodeModule();

            // DecisionTable dt = dtan.getDt();
            IOpenSourceCodeModule src = condition.getConditionEvaluator().getFormalSourceCode(condition);

            IParameterDeclaration[] pd = condition.getParams();

            IParameterDeclaration[] dtpd = dtan.referencedSignatureParams(condition);
            // IMethodSignature dtsignature = dt.getSignature();

            IMethodSignature newSignature = makeNewSignature(pd, dtpd, dtan);

            IOpenClass methodType = JavaOpenClass.getOpenClass(IntBoolExp.class);

            IOpenClass declaringClass = dtan.getDt().getDeclaringClass();

            OpenMethodHeader methodHeader = new OpenMethodHeader(condition.getName(), methodType, newSignature,
                    declaringClass);

            IBindingContext cxt = new ModuleBindingContext(openl.getBinder().makeBindingContext(),
                    (ModuleOpenClass) declaringClass);
            return OpenLManager.makeMethod(openl, src, methodHeader, cxt);

        }

        /**
         * @param i
         * @param j
         * @return
         */
        private IntBoolExp makeExpression(int rule, int cnum, DTAnalyzer dtan) {
            IDTCondition cond = cc[cnum];

            Object[] values = cond.getParamValues()[rule];

            if (values == null) {
                return new IntBoolExpConst(C, true);
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
                    args[i] = transformValue(name, cc[cnum], values[i - ndtArgs], dtan);
                }

            }

            Object instance = getInstance();
            return (IntBoolExp) cmethods[cnum].invoke(instance, args, openl.getVm().getRuntimeEnv());
        }

        /**
         * @return
         */
        private IntBoolExp[][] makeExpressions(DTAnalyzer dtan) {
            int nrules = dtvo.getDT().getNumberOfRules();
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

        private IMethodSignature makeNewSignature(IParameterDeclaration[] pd, IParameterDeclaration[] dtpd,
                DTAnalyzer dtan) {

            IParameterDeclaration[] pdd = new ParameterDeclaration[dtpd.length + pd.length];

            for (int i = 0; i < dtpd.length; i++) {
                IOpenClass newType = dtan.transformSignatureType(dtpd[i], dtvo);

                // IOpenClass newType = dtvo.getTransformer()
                // .transformSignatureType(dtpd[i]);
                if (newType == null) {
                    newType = dtpd[i].getType();
                }
                pdd[i] = new ParameterDeclaration(newType, dtpd[i].getName(), dtpd[i].getDirection());
            }

            for (int i = 0; i < pd.length; i++) {
                IOpenClass newType = dtvo.getTransformer().transformParameterType(pd[i]);
                pdd[i + dtpd.length] = newType == null ? pd[i] : new ParameterDeclaration(newType, pd[i].getName(),
                        pd[i].getDirection());
            }

            return new MethodSignature(pdd);
        }

        /**
         * @return
         */
        private IntExpArray makeVars(DTAnalyzer dtan) {

            List<IntExp> v = new ArrayList<IntExp>();

            // IMethodSignature dtsign = dtvo.getDT().getSignature();

            // int nargs = dtan.getNumberOfDTSignatureParams();

            for (Iterator<DTParamDescription> iterator = dtan.dtparams(); iterator.hasNext();) {
                DTParamDescription dtp = iterator.next();

                String vname = dtp.getOriginalDeclaration().getName();

                IntVar var = dtvo.getTransformer().makeSignatureVar(vname, dtp.getOriginalDeclaration().getType(), C);
                if (var != null) {
                    v.add(var);
                } else {
                    throw new RuntimeException("Could not create domain for " + vname);
                }
            }

            IntExpArray iary = new IntExpArray(C, v);
            return iary;
        }

        /**
         * @param name
         * @param condition
         * @param object
         * @return
         */
        private Object transformValue(String name, IDTCondition condition, Object value, DTAnalyzer dtan) {
            return dtvo.getTransformer().transformParameterValue(name, condition, value, C, dtan);
        }

        public DTValidationResult validateDT() {
            DecisionTable dt = dtvo.getDT();

            cc = dt.getConditionRows();

            IConditionSelector sel = dtvo.getSelector();

            if (sel != null) {
                cc = OpenIterator.fromArray(cc).select(sel).asList().toArray(new IDTCondition[0]);
            }

            if (cc.length == 0) {
                return new DTValidationResult(dt, new DTOverlapping[0], new DTUncovered[0]);
            }

            DTAnalyzer dtan = new DTAnalyzer(dt);

            for (int i = 0; i < cc.length; i++) {
                if (dtan.containsFormula(cc[i])) {
                    return new DTValidationResult(dt, new DTOverlapping[0], new DTUncovered[0]);
                }

            }

            cmethods = new IOpenMethod[cc.length];

            for (int i = 0; i < cc.length; i++) {
                cmethods[i] = makeCMethod(cc[i], dtan);
            }

            vars = makeVars(dtan);
            IntBoolExp[][] exp = makeExpressions(dtan);

            CDecisionTableImpl cdt = new CDecisionTableImpl(exp, vars);
            DTCheckerImpl dtc = new DTCheckerImpl(cdt);

            List<Uncovered> completeness = dtc.checkCompleteness();
            List<Overlapping> overlappings = dtc.checkOverlappings();

            System.out.println("C: " + completeness);
            System.out.println("O:" + overlappings);

            return new DTValidationResult(dtvo.getDT(), overlappings.toArray(new Overlapping[0]), completeness
                    .toArray(new Uncovered[0]), dtvo.getTransformer(), dtan);
        }

    }

    public static DTValidationResult validateDT(DecisionTable dt, Map<String, IDomainAdaptor> domains, IOpenClass type)
            throws Exception {

        return new DTValidator().validateDT(new DTValidatedObject(dt, domains), ((XlsModuleOpenClass) type).getOpenl());
    }

    public static DTValidationResult validateDT(String dtname, Map<String, IDomainAdaptor> domains, IOpenClass type)
            throws Exception {
        DecisionTable dt = (DecisionTable) AOpenClass.getSingleMethod(dtname, type.methods());

        return validateDT(dt, domains, type);
    }

    public IValidationResult validate(IValidatedObject ivo, OpenL openl) {
        return validateDT(ivo, openl);
    }

    public DTValidationResult validateDT(IValidatedObject ivo, OpenL openl) {
        return new ValidationAlgorithm((IDTValidatedObject) ivo, openl).validateDT();
    }
}
