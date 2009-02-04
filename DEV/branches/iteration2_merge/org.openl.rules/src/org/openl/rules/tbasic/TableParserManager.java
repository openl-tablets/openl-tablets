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
    public static TableParserManager instance(){
        return instance;
    }
    
    private EngineFactory<ITableParserManager> engineFactory;
    
    public TableParserManager(){
        engineFactory = new EngineFactory<ITableParserManager>("org.openl.xls",
                TableParserManager.class.getResource("TableParserSpecifications.xls"), ITableParserManager.class);

    }
    
    /*
     * (non-Javadoc)
     * 
     * @seeorg.openl.rules.tbasic.ITableParserManager#
     * getStructuredAlgorithmSpecification()
     */
    public TableParserSpecificationBean[] getStructuredAlgorithmSpecification() {
        TableParserSpecificationBean[] result = engineFactory.newInstance().getStructuredAlgorithmSpecification();

        return result;
    }
    
    public ConversionRuleBean[] getConversionRules() {
        ConversionRuleBean[] result = engineFactory.newInstance().getConversionRules();

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
    }

}
