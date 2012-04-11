package org.openl.rules.validator.dt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.binding.ILocalVar;
import org.openl.domain.IDomain;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.dt.IDecisionRow;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.ParameterDeclaration;

public class DTAnalyzer {
    DecisionTable dt;

    Map<IDecisionRow, ConditionAnalyzer> cdanMap;

    Map<String, DTParamDescription> usedDTParams = new HashMap<String, DTParamDescription>();

    static IOpenField getLocalField(IOpenField f) {
        if (f instanceof ILocalVar) {
            return f;
        }
        if (f instanceof OpenFieldDelegator) {
            OpenFieldDelegator d = (OpenFieldDelegator) f;
            return d.getField();

        }
        return f;
    }

    public DTAnalyzer(DecisionTable dt) {
        this.dt = dt;
        cdanMap = new HashMap<IDecisionRow, ConditionAnalyzer>();
        IDTCondition[] dtcc = dt.getConditionRows();
        for (int i = 0; i < dtcc.length; i++) {
            cdanMap.put(dtcc[i], new ConditionAnalyzer(dtcc[i]));
        }
    }

    public boolean containsFormula(IDecisionRow row) {
        Object[][] values = row.getParamValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                Object[] p = values[i];
                for (int j = 0; j < p.length; j++) {
                    if (p[j] != null && p[j] instanceof IOpenMethod) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Iterator<DTParamDescription> dtparams() {
        return usedDTParams.values().iterator();
    }

    public DecisionTable getDt() {
        return dt;
    }

    public IDomain<?> getParameterDomain(String parameterName, IDecisionRow condition) {
        return cdanMap.get(condition).getParameterDomain(parameterName);
    }

    public IDomain<?> getSignatureParameterDomain(String parameterName) {
        return usedDTParams.get(parameterName).getDomain();
    }

    public IParameterDeclaration[] referencedSignatureParams(IDecisionRow row) {
        IMethodSignature ims = dt.getSignature();

        CompositeMethod method = (CompositeMethod) row.getMethod();

        BindingDependencies deps = new BindingDependencies();

        method.updateDependency(deps);

        List<IParameterDeclaration> res = new ArrayList<IParameterDeclaration>();

        for (Iterator<IOpenField> iter = deps.getFieldsMap().values().iterator(); iter.hasNext();) {
            IOpenField f = iter.next();

            f = getLocalField(f);

            if (f instanceof ILocalVar) {
                for (int i = 0; i < ims.getNumberOfArguments(); i++) {
                    if (ims.getParameterName(i).equals(f.getName())) {
                        res.add(new ParameterDeclaration(ims.getParameterTypes()[i], ims.getParameterName(i)));
                    }
                }
            }
        }

        return res.toArray(new IParameterDeclaration[0]);

    }

    public void setDt(DecisionTable dt) {
        this.dt = dt;
    }

    public IOpenClass transformSignatureType(IParameterDeclaration parameterDeclaration, IDTValidatedObject dtvo) {

        DTParamDescription dpd = usedDTParams.get(parameterDeclaration.getName());
        if (dpd == null) {
            IOpenClass newType = dtvo.getTransformer().transformSignatureType(parameterDeclaration);
            dpd = new DTParamDescription(parameterDeclaration, newType);
            usedDTParams.put(parameterDeclaration.getName(), dpd);
        }

        return dpd.getNewType();
    }

}
