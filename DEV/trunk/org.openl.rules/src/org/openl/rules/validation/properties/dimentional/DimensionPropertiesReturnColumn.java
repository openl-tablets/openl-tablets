package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;

public class DimensionPropertiesReturnColumn implements IDecisionTableReturnColumn {
    
    private IOpenClass originalReturnType;
    private String originalTableName;
    private IMethodSignature originalSignature;
    private Map<String, IOpenClass> newIncomeParams;

    private static final String RET = "RET";
    private static final String RESULT_VAR = "result";
    
    public DimensionPropertiesReturnColumn(IOpenClass originalReturnType, String originalTableName, IMethodSignature originalSignature, Map<String, IOpenClass> newIncomeParams) {
        this.originalReturnType = originalReturnType;
        this.originalTableName = originalTableName;
        this.originalSignature = originalSignature;
        this.newIncomeParams = newIncomeParams;    
    }
    
    public String getParameterDeclaration() {        
        return String.format("%s %s", originalReturnType.getDisplayName(0), RESULT_VAR);
    }
    
    public String getCodeExpression() {
        return RESULT_VAR;
    }
    
    public String getTitle() {
        return RESULT_VAR.toUpperCase();
    }
    
    public String getRuleValue(int ruleIndex, int elementNum) {        
        return String.format("=%s(%s)", originalTableName, originalParamsThroughComma());
    }

    public IOpenClass getReturnType() {
        return originalReturnType;
    }
    
    private String originalParamsThroughComma() {
        List<String> values = new ArrayList<String>();        
        for (int i = 0; i < originalSignature.getNumberOfParameters(); i++) {            
            values.add(originalSignature.getParameterName(i));            
        }
        return StringTool.listToStringThroughCommas(values);
    }

    public String paramsThroughComma() {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(originalParamsWithTypesThroughComma());  
        strBuf.append(", ");
        strBuf.append(paramsWithTypesThroughComma(newIncomeParams));  
        return strBuf.toString();
    }
    
    private String originalParamsWithTypesThroughComma() {        
        List<String> values = new ArrayList<String>();        
        for (int j = 0; j < originalSignature.getNumberOfParameters(); j++) {
            values.add(String.format("%s %s", originalSignature.getParameterType(j).getInstanceClass().getSimpleName(), 
                    originalSignature.getParameterName(j)));            
        }   
        return StringTool.listToStringThroughCommas(values);
    }
    
    private String paramsWithTypesThroughComma(Map<String, IOpenClass> params) {
        List<String> values = new ArrayList<String>();
        for (Map.Entry<String, IOpenClass> param : params.entrySet()) {
            values.add(String.format("%s %s", param.getValue().getInstanceClass().getSimpleName(), param.getKey()));
        }
        
        return StringTool.listToStringThroughCommas(values);
    }
    
    /**
     * default behavior says that just one value can exist for any rule.
     */
    public int getMaxNumberOfValuesForRules() {
        return 1;
    }
    
    public String getRuleValue(int ruleIndex) {
        return getRuleValue(ruleIndex, 0);
    }
    
    public boolean isArrayCondition() {
        return false;
    }
    
    public String getColumnType() {
        return RET;
    }

}
