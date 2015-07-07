package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
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
     * Name of method in overloaded tables group.
     */
    private String methodName;
    
    /**
     * Signature of the member of overloaded tables group.
     */
    private IMethodSignature originalSignature;
    
    private static final String RESULT_VAR = "result";
    
    /**for tests*/
    protected DispatcherTableReturnColumn(IOpenClass originalReturnType, String methodName,
            IMethodSignature originalSignature) {
        this.originalReturnType = originalReturnType;
        this.methodName = methodName;
        this.originalSignature = originalSignature;
    }
    
    public DispatcherTableReturnColumn(MatchingOpenMethodDispatcher dispatcher) {
        this(dispatcher.getType(), dispatcher.getName(), dispatcher.getSignature());
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
        return String.format("=%s%s%d(%s)",
            methodName,
            TableSyntaxNodeDispatcherBuilder.AUXILIARY_METHOD_DELIMETER,
            ruleIndex,
            originalParamsThroughComma());
    }

    public IOpenClass getReturnType() {
        return originalReturnType;
    }
    
    private String originalParamsThroughComma() {
        String result = StringUtils.EMPTY;
        List<String> values = new ArrayList<String>();        
        for (int i = 0; i < originalSignature.getNumberOfParameters(); i++) {
            values.add(TableSyntaxNodeDispatcherBuilder.getDispatcherParameterNameForOriginalParameter(originalSignature.getParameterName(i)));
        }
        if (!values.isEmpty()) {
            result = StringTool.listToStringThroughSymbol(values, ","); 
        }
        return result; 
    }

    public String paramsThroughComma() {
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
                values.add(String.format("%s %s",
                    originalSignature.getParameterType(j).getInstanceClass().getSimpleName(),
                    TableSyntaxNodeDispatcherBuilder.getDispatcherParameterNameForOriginalParameter(originalSignature.getParameterName(j))));
            }           
        }   
        if (values.size() > 0) {
            result = StringTool.listToStringThroughSymbol(values, ","); 
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
    
    public String getColumnType() {
        return DecisionTableColumnHeaders.RETURN.getHeaderKey();
    }

}
