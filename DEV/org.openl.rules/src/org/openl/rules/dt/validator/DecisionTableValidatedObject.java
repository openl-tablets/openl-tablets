/**
 * Created Feb 8, 2007
 */
package org.openl.rules.dt.validator;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.dt.type.domains.EnumDomainAdaptor;
import org.openl.rules.dt.type.domains.IDomainAdaptor;
import org.openl.rules.dt.type.domains.IntRangeDomainAdaptor;
import org.openl.rules.dt.type.domains.JavaEnumDomainAdaptor;
import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaEnumDomain;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.IntBoolVar;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;

/**
 * @author snshor
 *
 */
public class DecisionTableValidatedObject implements IDecisionTableValidatedObject, IConditionTransformer {

    private IDecisionTable decisionTable;
    private Map<String, IDomainAdaptor> domainMap;

    public DecisionTableValidatedObject(IDecisionTable decisionTable) {
        this.decisionTable = decisionTable;
    }

    public DecisionTableValidatedObject(IDecisionTable decisionTable, Map<String, IDomainAdaptor> domainMap) {
        this.decisionTable = decisionTable;
        this.domainMap = domainMap;
    }

    public synchronized Map<String, IDomainAdaptor> getDomains() {
        if (domainMap == null) {
            domainMap = makeDomains(decisionTable);
        }
        return domainMap;
    }

    public IDecisionTable getDecisionTable() {
        return decisionTable;
    }

    public IConditionSelector getSelector() {
        return null;
    }

    public IConditionTransformer getTransformer() {
        return this;
    }

    private IDomainAdaptor makeDomainAdaptor(IDomain<?> domain) {        
        IDomainAdaptor result = null;
        if (domain instanceof EnumDomain<?>) {
            result = new EnumDomainAdaptor(((EnumDomain<?>) domain));
        } else if (domain instanceof IntRangeDomain) {
            IntRangeDomain irange = (IntRangeDomain) domain;
            result = new IntRangeDomainAdaptor(irange);
        } else if (domain instanceof JavaEnumDomain) {
            result = new JavaEnumDomainAdaptor((JavaEnumDomain)domain);
        }
        return result;
    }

    private Map<String, IDomainAdaptor> makeDomains(IDecisionTable dt2) {
        return new HashMap<String, IDomainAdaptor>();
    }

    public IntVar makeSignatureVar(String parameterName, IOpenClass paramType, Constrainer constrainer) {
        IDomainAdaptor domain = getDomains().get(parameterName);
        if (domain == null) {
            if (paramType.getDomain() != null) {
                domain = makeDomainAdaptor(paramType.getDomain());
            } else if (paramType.getInstanceClass() == boolean.class || paramType.getInstanceClass() == Boolean.class) {
                return constrainer.addIntBoolVar(parameterName);
            }
        }

        if (domain != null) {
            return constrainer.addIntVar(domain.getMin(), domain.getMax(), parameterName);
        }

        Log.warn("Parameter " + parameterName + " has no domain");

        return null;

    }

    public IOpenClass transformParameterType(IParameterDeclaration parameterDeclaration) { 

        Class<?> instanceClass = parameterDeclaration.getType().getInstanceClass();
        
        if (instanceClass == String.class || instanceClass == Date.class) {
            return JavaOpenClass.INT;
        }

        if (instanceClass == IntRange.class) {
            return JavaOpenClass.getOpenClass(CtrIntRange.class);
        }
        
        if (instanceClass == boolean.class || instanceClass == Boolean.class) {
            return JavaOpenClass.INT;
        }
        
        if (instanceClass.isEnum()) {
            return JavaOpenClass.INT;
        }
        
        if (instanceClass.isArray())
            return  JavaOpenClass.getOpenClass(int[].class);
        

        return null;
    }
        
    public Object transformLocalParameterValue(String name, IBaseCondition condition, Object value, DecisionTableAnalyzer dtan) {
        
        if (value != null && value.getClass().isArray())
        {
            int[] res = new int[Array.getLength(value)];
            for (int i = 0; i < res.length; i++) {
                res[i] = (Integer)transformSingleLocalParameterValue(name, condition, Array.get(value, i), dtan); 
            }
            
            return res;
        }   
        else return transformSingleLocalParameterValue(name, condition, value, dtan);
    } 
        
  public Object transformSingleLocalParameterValue(String name, IBaseCondition condition, Object value, DecisionTableAnalyzer dtan) {
        
        Object result = value;
        if (value instanceof IntRange) {
            IntRange intr = (IntRange) value;
            return new CtrIntRange(intr.getMin(), intr.getMax());
        }
        
        // at first search domains in those that were defined by user.
        String uniquePname = DecisionTableValidator.getUniqueConditionParamName(condition, name);
        IDomainAdaptor domainAdaptor = getDomains().get(uniquePname);
        if (domainAdaptor == null)
            domainAdaptor = getDomains().get(name);
        
        if (domainAdaptor != null) {
            result = domainAdaptor.getIndex(value);
        } else { // then search domains from its type.
            IDomain<?> domain = dtan.getParameterDomain(name, condition);
            if (domain != null) {
                IDomainAdaptor domainAdapt = makeDomainAdaptor(domain);
                result = domainAdapt.getIndex(value);
            } else {
                if (!(value instanceof Integer)) { // integer don`t need to be converted. so the original value 
                                                   // will be returned. in other cases throws an exception.
                    throw new OpenLRuntimeException(String.format("Could not create domain for %s", name));
                }
            }
        }
        return result;
    }

    public IOpenClass transformSignatureType(IParameterDeclaration parameterDeclaration) {

        Class<?> instanceClass = parameterDeclaration.getType().getInstanceClass();
        if (instanceClass == String.class || instanceClass == Date.class) {
            return JavaOpenClass.getOpenClass(IntExp.class);
        }

        if (instanceClass == int.class || instanceClass == Integer.class) {
            return JavaOpenClass.getOpenClass(IntExp.class);
        }

        if (instanceClass == boolean.class || instanceClass == Boolean.class) {
            return JavaOpenClass.getOpenClass(IntBoolVar.class);
        }
        
        if (instanceClass.isEnum()) {
            return JavaOpenClass.getOpenClass(IntExp.class);
        }

        return null;
    }

    public Object transformSignatureValueBack(String name, int intValue, DecisionTableAnalyzer dtAnalyzer) {        
        Object result = intValue;
        
        DecisionTableParamDescription pd = dtAnalyzer.getUsedParams().get(name);

        Class<?> instanceClass = pd.getParameterDeclaration().getType().getInstanceClass();

        if (instanceClass == boolean.class || instanceClass == Boolean.class) {
            return intValue == 1 ? "true" : "false";
        }
        
        // at first search domains in those that were defined by user.
        IDomainAdaptor domainAdapt = getDomains().get(name);
        if (domainAdapt != null) {
            result = domainAdapt.getValue(intValue);
        } else { // then search domains from its type.
            IDomain<?> domain = dtAnalyzer.getSignatureParameterDomain(name);
            if (domain != null) {
                IDomainAdaptor domainAdaptor = makeDomainAdaptor(domain);
                result = domainAdaptor.getValue(intValue);
            } else {
                result = intValue;
            }
        }
        
        return result;
    }

    
    
    public boolean isOverrideAscending() {
        //1. if return type is void, return false
        if (decisionTable.getMethod().getType() == JavaOpenClass.VOID)
            return false;
        
        return true;
    }

}
