/**
 *
 */
package org.openl.rules.tbasic;

import java.net.URL;
import java.util.List;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.tbasic.compile.ConversionRuleBean;

/**
 * @author User
 *
 */
public final class AlgorithmTableParserManager implements IAlgorithmTableParserManager {
    // To make class serializable, change synchronization

    private static volatile AlgorithmTableParserManager INSTANCE;

    private static Object synchObjectForInstance = new Object();

    private final IAlgorithmTableParserManager rulesWrapperInstance;

    private volatile ConversionRuleBean[] convertionRules;

    private volatile ConversionRuleBean[] fixedConvertionRules;

    private Object synchObjectForConvertionRules = new Object();

    private Object synchObjectForFixedConvertionRules = new Object();

    public static AlgorithmTableParserManager getInstance() {
        lazyLoadInstance();
        return INSTANCE;
    }

    private static void lazyLoadInstance() {
        if (INSTANCE == null) {
            synchronized (synchObjectForInstance) {
                if (INSTANCE == null) {
                    INSTANCE = new AlgorithmTableParserManager();
                }
            }
        }
    }

    private AlgorithmTableParserManager() {
        URL sourceFile = AlgorithmTableParserManager.class.getResource("AlgorithmTableSpecification.xls");

        RulesEngineFactory<IAlgorithmTableParserManager> engineFactory = new RulesEngineFactory<>(
            sourceFile,
            IAlgorithmTableParserManager.class);
        engineFactory.setExecutionMode(true);

        rulesWrapperInstance = (IAlgorithmTableParserManager) engineFactory.makeInstance();
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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.tbasic.ITableParserManager# getStructuredAlgorithmSpecification()
     */
    @Override
    public TableParserSpecificationBean[] getAlgorithmSpecification() {
        TableParserSpecificationBean[] result = rulesWrapperInstance.getAlgorithmSpecification();

        return result;
    }

    @Override
    public ConversionRuleBean[] getConversionRules() {
        lazyLoadConversionRules();

        return convertionRules;
    }

    public ConversionRuleBean[] getFixedConversionRules() {
        lazyLoadFixedConvertionRules();

        return fixedConvertionRules;
    }

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

    @Override
    public String whatIsOperationsGroupName(List<String> groupedOperationNames) {
        return rulesWrapperInstance.whatIsOperationsGroupName(groupedOperationNames);
    }

    @Override
    public String[] whatOperationsToGroup(String keyword) {
        return rulesWrapperInstance.whatOperationsToGroup(keyword);
    }
}
