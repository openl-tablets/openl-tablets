package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.util.StringTool;

/**
 * This class is used to build dispatcher table by dimensional properties for the group of overloaded tables.
 * In this table in return column there are calls for original table from the group for every rule.
 *  
 * @author DLiauchuk
 *
 */
public class DispatcherTableReturnColumn implements IDecisionTableReturnColumn {
    
    /**
     * Return type of the member of overloaded tables group.
     */
    private IOpenClass originalReturnType;
    
    /**
     * Table names of the auxiliary methods for each method on overloaded tables group.
     */
    private List<String> auxiliaryMethodNames;
    
    /**
     * Signature of the member of overloaded tables group.
     */
    private IMethodSignature originalSignature;
    
    /**
     * As new income params to newly created dispatcher table are used params from context.
     */
    private Map<String, IOpenClass> newIncomeParams;
    
    private static final String RESULT_VAR = "result";
    
    /**for tests*/
    protected DispatcherTableReturnColumn () {        
    }
    
    protected DispatcherTableReturnColumn(IOpenClass originalReturnType, List<String> auxiliaryMethodNames, 
            IMethodSignature originalSignature, Map<String, IOpenClass> newIncomeParams) {
        this.originalReturnType = originalReturnType;
        this.auxiliaryMethodNames = auxiliaryMethodNames;
        this.originalSignature = originalSignature;
        this.newIncomeParams = new HashMap<String, IOpenClass>(newIncomeParams);    
    }
    
    public DispatcherTableReturnColumn(MatchingOpenMethodDispatcher dispatcher, 
            Map<String, IOpenClass> newIncomeParams) {        
        this(dispatcher.getType(), getAuxiliaryMethodNames(dispatcher), 
            dispatcher.getSignature(), newIncomeParams);        
    }
    
    private static List<String> getAuxiliaryMethodNames(MatchingOpenMethodDispatcher dispatcher){
        List<IOpenMethod> sortedByPriorityMethods = dispatcher.getSortedByPriorityMethods();
        List<String> methodNames = new ArrayList<String>(sortedByPriorityMethods.size());
        for(IOpenMethod method : sortedByPriorityMethods){
            IOpenMethod auxiliaryMethod = dispatcher.getAuxiliaryMethodForCandidate(method);
            methodNames.add(auxiliaryMethod.getName());
        }
        return methodNames;
    }

    public void setOriginalReturnType(IOpenClass originalReturnType) {
        this.originalReturnType = originalReturnType;
    }
    
    public void setOriginalSignature(IMethodSignature originalSignature) {
        this.originalSignature = originalSignature;
    }

    public void setNewIncomeParams(Map<String, IOpenClass> newIncomeParams) {
        this.newIncomeParams = new HashMap<String, IOpenClass>(newIncomeParams);
    }

    public String getParameterDeclaration() {        
        return String.format("%s %s", getReturnType().getDisplayName(0), getCodeExpression());
    }
    
    public String getCodeExpression() {
        return RESULT_VAR;
    }
    
    public String getTitle() {
        return getCodeExpression().toUpperCase();
    }
    
    public String getRuleValue(int ruleIndex, int elementNum) {        
        return String.format("=%s(%s)",
            auxiliaryMethodNames.get(ruleIndex),
            originalParamsThroughComma());
    }

    public IOpenClass getReturnType() {
        return originalReturnType;
    }
    
    /**protected for tests*/
    protected String originalParamsThroughComma() {
        String result = StringUtils.EMPTY;
        List<String> values = new ArrayList<String>();        
        for (int i = 0; i < originalSignature.getNumberOfParameters(); i++) {            
            values.add(originalSignature.getParameterName(i));            
        }
        if (!values.isEmpty()) {
            result = StringTool.listToStringThroughSymbol(values, ","); 
        }
        return result; 
    }

    public String paramsThroughComma() {
        List<String> values = new ArrayList<String>();
        String originalParamsThroughComma = originalParamsWithTypesThroughComma();
        
        // add original parameters of the method
        //
        if (StringUtils.isNotBlank(originalParamsThroughComma)) {
            values.add(originalParamsThroughComma);
        }
        
        // add new income parameters through comma
        //
        String newParamsThroughComma = paramsWithTypesThroughComma(newIncomeParams);
        if (StringUtils.isNotBlank(newParamsThroughComma)) {
            values.add(newParamsThroughComma);
        }
        
        return StringTool.listToStringThroughSymbol(values, ",");
    }
    
    private String originalParamsWithTypesThroughComma() {
        String result = StringUtils.EMPTY;
        List<String> values = new ArrayList<String>();        
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) { 
            if (!(originalSignature.getParameterType(j) instanceof NullOpenClass)) { // on compare in repository tutorial10,
                                                                                     // all original parameter types are 
                                                                                     // instances of NullOpenClass.
                                                                                     // it causes NullPointerException. 
                                                                                     // On compare we don`t need to build
                                                                                     // and execute validation tables at 
                                                                                     // all during binding.
                values.add(String.format("%s %s", originalSignature.getParameterType(j).getInstanceClass().getSimpleName(), 
                    originalSignature.getParameterName(j)));
            }           
        }   
        if (values.size() > 0) {
            result = StringTool.listToStringThroughSymbol(values, ","); 
        }
        return result; 
    }
    
    private String paramsWithTypesThroughComma(Map<String, IOpenClass> params) {    
        String result = StringUtils.EMPTY;
        List<String> values = new ArrayList<String>();
        for (Map.Entry<String, IOpenClass> param : params.entrySet()) {
            values.add(String.format("%s %s", param.getValue().getInstanceClass().getSimpleName(), param.getKey()));
        }
        if (!values.isEmpty()) {
            result = StringTool.listToStringThroughSymbol(values, ",");; 
        }        
        return result; 
    }
    
    public int getNumberOfLocalParameters() {
        /**
         * For return column only one local parameter is possible.
         */
        return 1;
    }
    
    public String getRuleValue(int ruleIndex) {
        return getRuleValue(ruleIndex, 0);
    }
    
    public boolean isArrayCondition() {
        return false;
    }
    
    public String getColumnType() {
        return DecisionTableColumnHeaders.RETURN.getHeaderKey();
    }

}
