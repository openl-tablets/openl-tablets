/**
 * 
 */
package org.openl.rules.tbasic;

import org.openl.runtime.EngineFactory;

/**
 * @author User
 * 
 */
public class TableParserManager implements ITableParserManager {
    /*
     * (non-Javadoc)
     * 
     * @seeorg.openl.rules.tbasic.ITableParserManager#
     * getStructuredAlgorithmSpecification()
     */
    public TableParserSpecificationBean[] getStructuredAlgorithmSpecification() {
        EngineFactory<ITableParserManager> engineFactory = new EngineFactory<ITableParserManager>("org.openl.xls",
                TableParserManager.class.getResource("TableParserSpecifications.xls"), ITableParserManager.class);

        TableParserSpecificationBean[] result = engineFactory.newInstance().getStructuredAlgorithmSpecification();

        return result;
    }

    public static void main(String[] args) {
        TableParserManager instance = new TableParserManager();
        TableParserSpecificationBean[] result = instance.getStructuredAlgorithmSpecification();
        for (TableParserSpecificationBean bean : result) {
            System.out.println(bean.getKeyword());
        }
    }

}
