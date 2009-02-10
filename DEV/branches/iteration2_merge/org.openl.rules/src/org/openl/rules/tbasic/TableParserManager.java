/**
 * 
 */
package org.openl.rules.tbasic;

import org.openl.rules.tbasic.compile.ConversionRuleBean;
import org.openl.runtime.EngineFactory;

/**
 * @author User
 * 
 */
public class TableParserManager implements ITableParserManager {
    private static TableParserManager instance = new TableParserManager();
    private ITableParserManager rulesWrapperInstance;

    public static TableParserManager instance() {
        return instance;
    }

    public TableParserManager() {
        EngineFactory<ITableParserManager> engineFactory = new EngineFactory<ITableParserManager>("org.openl.xls", TableParserManager.class
                .getResource("TableParserSpecifications.xls"), ITableParserManager.class);
        rulesWrapperInstance = engineFactory.newInstance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.rules.tbasic.ITableParserManager#
     * getStructuredAlgorithmSpecification()
     */
    public TableParserSpecificationBean[] getStructuredAlgorithmSpecification() {
        TableParserSpecificationBean[] result = rulesWrapperInstance.getStructuredAlgorithmSpecification();

        return result;
    }

    public ConversionRuleBean[] getConversionRules() {
        ConversionRuleBean[] result = rulesWrapperInstance.getConversionRules();

        return result;
    }
    
    public ConversionRuleBean[] getFixedConversionRules() {
        ConversionRuleBean[] draftConvertionRules = getConversionRules();
        return fixBrokenValues(draftConvertionRules);
    }
    
    private ConversionRuleBean[] fixBrokenValues(ConversionRuleBean[] conversionRules) {
        for (ConversionRuleBean conversionRule : conversionRules){
            fixBrokenValues(conversionRule.getOperationType());
            fixBrokenValues(conversionRule.getOperationParam1());
            fixBrokenValues(conversionRule.getOperationParam2());
            fixBrokenValues(conversionRule.getLabel());
        }
        return conversionRules;
    }

    private void fixBrokenValues(String[] label) {
        for (int i = 0; i < label.length; i++){
            if (label[i].toUpperCase().equals("N/A")){
                label[i] = null;
            }
        }
        
    }

    public String[] whatOperationsToGroup(String keyword) {
        String[] result = rulesWrapperInstance.whatOperationsToGroup(keyword);

        return result;
    }
    
    public String whatIsOperationsGroupName(String[] groupedOperationNames){
        String result = rulesWrapperInstance.whatIsOperationsGroupName(groupedOperationNames);
        return result;
    }

    public static void main(String[] args) {
        TableParserSpecificationBean[] result = TableParserManager.instance().getStructuredAlgorithmSpecification();
        for (TableParserSpecificationBean bean : result) {
            System.out.println(bean.getKeyword());
        }

        ConversionRuleBean[] conversionRules = TableParserManager.instance().getConversionRules();
        for (ConversionRuleBean bean : conversionRules) {
            System.out.println(bean.getOperation());
        }
        
        String[] operationNames = TableParserManager.instance().whatOperationsToGroup("IF");
        for (String operationName : operationNames) {
            System.out.println(operationName);
        }
    }

}
