/**
 * 
 */
package org.openl.rules.tbasic;

import java.net.URL;
import java.util.List;

import org.openl.rules.tbasic.compile.ConversionRuleBean;
import org.openl.runtime.EngineFactory;

/**
 * @author User
 * 
 */
public final class AlgorithmTableParserManager implements IAlgorithmTableParserManager {
    // To make class serializable, change synchronization

    private static volatile AlgorithmTableParserManager instance;
    private final IAlgorithmTableParserManager rulesWrapperInstance;

    public static AlgorithmTableParserManager instance() {
        lazyLoadInstance();
        return instance;
    }

    private AlgorithmTableParserManager() {
        String sourceType = "org.openl.xls";
        URL sourceFile = AlgorithmTableParserManager.class.getResource("AlgorithmTableSpecification.xls");

        EngineFactory<IAlgorithmTableParserManager> engineFactory = new EngineFactory<IAlgorithmTableParserManager>(
                sourceType, sourceFile, IAlgorithmTableParserManager.class);
        rulesWrapperInstance = engineFactory.newInstance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.rules.tbasic.ITableParserManager#
     * getStructuredAlgorithmSpecification()
     */
    public TableParserSpecificationBean[] getAlgorithmSpecification() {
        TableParserSpecificationBean[] result = rulesWrapperInstance.getAlgorithmSpecification();

        return result;
    }

    private volatile ConversionRuleBean[] convertionRules;

    public ConversionRuleBean[] getConversionRules() {
        lazyLoadConversionRules();

        return convertionRules;
    }

    private volatile ConversionRuleBean[] fixedConvertionRules;

    public ConversionRuleBean[] getFixedConversionRules() {
        lazyLoadFixedConvertionRules();

        return fixedConvertionRules;
    }

    public String[] whatOperationsToGroup(String keyword) {
        return rulesWrapperInstance.whatOperationsToGroup(keyword);
    }

    public String whatIsOperationsGroupName(List<String> groupedOperationNames) {
        return rulesWrapperInstance.whatIsOperationsGroupName(groupedOperationNames);
    }

    private static Object synchObjectForInstance = new Object();

    private static void lazyLoadInstance() {
        if (instance == null) {
            synchronized (synchObjectForInstance) {
                if (instance == null) {
                    instance = new AlgorithmTableParserManager();
                }
            }
        }
    }

    private Object synchObjectForConvertionRules = new Object();

    /**
     * 
     */
    private void lazyLoadConversionRules() {
        if (convertionRules == null) {
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
        if (fixedConvertionRules == null) {
            synchronized (synchObjectForFixedConvertionRules) {
                if (fixedConvertionRules == null) {
                    ConversionRuleBean[] draftConvertionRules = getConversionRules().clone();
                    fixedConvertionRules = fixBrokenValues(draftConvertionRules);
                }
            }
        }
    }

    private ConversionRuleBean[] fixBrokenValues(ConversionRuleBean[] conversionRules) {
        for (ConversionRuleBean conversionRule : conversionRules) {
            fixBrokenValues(conversionRule.getOperationType());
            fixBrokenValues(conversionRule.getOperationParam1());
            fixBrokenValues(conversionRule.getOperationParam2());
            fixBrokenValues(conversionRule.getLabel());
            fixBrokenValues(conversionRule.getNameForDebug());
        }
        return conversionRules;
    }

    private void fixBrokenValues(String[] label) {
        for (int i = 0; i < label.length; i++) {
            if (label[i].equalsIgnoreCase("N/A")) {
                label[i] = null;
            } else if (label[i].equalsIgnoreCase("\"\"")) {
                label[i] = "";
            }
        }

    }
}
