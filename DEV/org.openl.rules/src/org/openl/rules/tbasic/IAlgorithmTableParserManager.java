package org.openl.rules.tbasic;

import java.util.List;

import org.openl.rules.tbasic.compile.ConversionRuleBean;

public interface IAlgorithmTableParserManager {

    TableParserSpecificationBean[] getAlgorithmSpecification();

    ConversionRuleBean[] getConversionRules();

    String whatIsOperationsGroupName(List<String> groupedOperationNames);

    String[] whatOperationsToGroup(String keyword);

}