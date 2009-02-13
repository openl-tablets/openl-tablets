/**
 * 
 */
package org.openl.rules.tbasic;

import java.util.Arrays;
import java.util.List;

import org.openl.rules.tbasic.compile.ConversionRuleBean;
import org.openl.runtime.EngineFactory;

/**
 * @author User
 * 
 */
public class TableParserManager implements ITableParserManager {
    // To make class serializable, change synchronization
    
    private static TableParserManager instance;
    private ITableParserManager rulesWrapperInstance;

    public static TableParserManager instance() {
        lazyLoadInstance();
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
    
    private ConversionRuleBean[] convertionRules;

    public ConversionRuleBean[] getConversionRules() {
        lazyLoadConversionRules();

        return convertionRules;
    }

    private ConversionRuleBean[] fixedConvertionRules;
    
    public ConversionRuleBean[] getFixedConversionRules() {
        lazyLoadFixedConvertionRules();
        
        return fixedConvertionRules;
    }

    public String[] whatOperationsToGroup(String keyword) {
        String[] result = rulesWrapperInstance.whatOperationsToGroup(keyword);

        return result;
    }
    
    public String whatIsOperationsGroupName(List<String> groupedOperationNames){
        String result = rulesWrapperInstance.whatIsOperationsGroupName(groupedOperationNames);
        return result;
    }
    
    private static Object synchObjectForInstance = new Object();
    
    private static void lazyLoadInstance() {
        if (instance == null){
            synchronized (synchObjectForInstance) {
                if (instance == null) {
                    instance = new TableParserManager();
                }
            }
        }
    }
    
    private Object synchObjectForConvertionRules = new Object();
    
    /**
     * 
     */
    private void lazyLoadConversionRules() {
        if (convertionRules == null){
            synchronized (synchObjectForConvertionRules) {
                if (convertionRules == null) {
                    convertionRules = rulesWrapperInstance.getConversionRules();
                }
            }
        }
    }
    
    private Object synchObjectForFixedConvertionRules = new Object();
    
    /**
     * 
     */
    private void lazyLoadFixedConvertionRules() {
        if (fixedConvertionRules == null){
            synchronized (synchObjectForFixedConvertionRules) {
                if (fixedConvertionRules == null){
                    ConversionRuleBean[] draftConvertionRules = getConversionRules().clone();
                    fixedConvertionRules = fixBrokenValues(draftConvertionRules);
                }
            }
        }
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
        
        System.out.println("----------------------------------------------");
        
        String groupOperationsName1 = TableParserManager.instance().whatIsOperationsGroupName(Arrays.asList(new String[]{"IF"}));
        System.out.println(groupOperationsName1);
        
        String groupOperationsName2 = TableParserManager.instance().whatIsOperationsGroupName(Arrays.asList(new String[]{"IF", "ELSE"}));
        System.out.println(groupOperationsName2);
        
        String groupOperationsName3 = TableParserManager.instance().whatIsOperationsGroupName(Arrays.asList(new String[]{"IF", "ELSE", "END IF"}));
        System.out.println(groupOperationsName3);
        
        String groupOperationsName4 = TableParserManager.instance().whatIsOperationsGroupName(Arrays.asList(new String[]{"IF", "END IF"}));
        System.out.println(groupOperationsName4);
        
        String groupOperationsName5 = TableParserManager.instance().whatIsOperationsGroupName(Arrays.asList(new String[]{"WHILE"}));
        System.out.println(groupOperationsName5);
        
        String groupOperationsName6 = TableParserManager.instance().whatIsOperationsGroupName(Arrays.asList(new String[]{"WHILE", "END WHILE"}));
        System.out.println(groupOperationsName6);
    }

}
