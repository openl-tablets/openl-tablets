package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openl.rules.tbasic.AlgorithmTableParserManager;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class ConversionRulesController {
    private static ConversionRulesController instance;
    private ConversionRuleBean[] conversionRules;

    public static ConversionRulesController getInstance() {
        if (instance == null) {
            return new ConversionRulesController();
        }
        return instance;
    }

    private ConversionRulesController() {
        conversionRules = AlgorithmTableParserManager.getInstance().getFixedConversionRules();
    }

    /**
     * @throws BoundError
     */
    public ConversionRuleBean getConvertionRule(List<AlgorithmTreeNode> nodesToCompile) throws SyntaxNodeException {
        assert !nodesToCompile.isEmpty();

        List<String> groupedOperationNames = new ArrayList<>(nodesToCompile.size());

        for (AlgorithmTreeNode node : nodesToCompile) {
            groupedOperationNames.add(node.getSpecificationKeyword().toUpperCase());
        }

        // Resolve the name of the group defined in the Algorithm Specification
        //
        String operationGroupName = AlgorithmTableParserManager.getInstance()
            .whatIsOperationsGroupName(groupedOperationNames);

        boolean isMultilineOperation;
        // we assume that all the operations are either all multiline or not
        isMultilineOperation = nodesToCompile.get(0).getSpecification().isMultiline();

        for (ConversionRuleBean conversionRule : conversionRules) {
            if (conversionRule.getOperation()
                .equals(operationGroupName) && (conversionRule.isMultiLine() == isMultilineOperation)) {
                return conversionRule;
            }
        }

        // No conversion rule found.

        List<String> predecessorOperations = Arrays
            .asList(nodesToCompile.get(0).getSpecification().getPredecessorOperations());
        String errorMessage = String.format(
            "The operations sequence is wrong: %2$s. Operations %1$s must precede the %2$s",
            predecessorOperations,
            groupedOperationNames);
        IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation().asSourceCodeModule();
        throw SyntaxNodeExceptionUtils.createError(errorMessage, errorSource);
    }

}
