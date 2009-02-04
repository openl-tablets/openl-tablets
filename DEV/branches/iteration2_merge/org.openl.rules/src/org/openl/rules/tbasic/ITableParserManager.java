package org.openl.rules.tbasic;

import org.openl.rules.tbasic.compile.ConversionRuleBean;

public interface ITableParserManager {

    public abstract TableParserSpecificationBean[] getStructuredAlgorithmSpecification();
    public abstract ConversionRuleBean[] getConversionRules();

}