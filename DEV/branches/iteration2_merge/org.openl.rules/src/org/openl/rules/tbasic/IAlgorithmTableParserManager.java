package org.openl.rules.tbasic;

import java.util.List;

import org.openl.rules.tbasic.compile.ConversionRuleBean;

public interface IAlgorithmTableParserManager {

    TableParserSpecificationBean[] getAlgorithmSpecification();
    ConversionRuleBean[] getConversionRules();
    String[] whatOperationsToGroup(String keyword);
    String whatIsOperationsGroupName(List<String> groupedOperationNames);

}