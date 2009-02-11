package org.openl.rules.tbasic;

import java.util.List;

import org.openl.rules.tbasic.compile.ConversionRuleBean;

public interface ITableParserManager {

    TableParserSpecificationBean[] getStructuredAlgorithmSpecification();
    ConversionRuleBean[] getConversionRules();
    String[] whatOperationsToGroup(String keyword);
    String whatIsOperationsGroupName(List<String> groupedOperationNames);

}