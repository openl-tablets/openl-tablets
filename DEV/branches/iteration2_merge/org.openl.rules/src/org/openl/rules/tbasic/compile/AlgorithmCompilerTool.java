package org.openl.rules.tbasic.compile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.openl.IOpenSourceCodeModule;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmRow;
import org.openl.rules.tbasic.AlgorithmTableParserManager;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class AlgorithmCompilerTool {

    /**
     * @param nodesToProcess
     * @param firstNodeIndex
     * @return
     */
    public static int getLinkedNodesGroupSize(List<AlgorithmTreeNode> nodesToProcess, int firstNodeIndex) {
        int linkedNodesGroupSize = 1; // just one operation by default

        AlgorithmTreeNode currentNodeToProcess = nodesToProcess.get(firstNodeIndex);
        String currentNodeKeyword = currentNodeToProcess.getSpecification().getKeyword();

        String[] operationNamesToGroup = AlgorithmTableParserManager.instance().whatOperationsToGroup(
                currentNodeKeyword);

        if (operationNamesToGroup != null) {
            List<String> operationsToGroupWithCurrent = Arrays.asList(operationNamesToGroup);

            for (; linkedNodesGroupSize < nodesToProcess.size() - firstNodeIndex; linkedNodesGroupSize++) {
                AlgorithmTreeNode groupCandidateNode = nodesToProcess.get(firstNodeIndex + linkedNodesGroupSize);
                if (!operationsToGroupWithCurrent.contains(groupCandidateNode.getSpecification().getKeyword())) {
                    break;
                }
            }
        }

        return linkedNodesGroupSize;
    }

    /**
     * @param nodesToCompile
     * @return
     * @throws BoundError
     */
    public static List<AlgorithmTreeNode> getNestedInstructionsBlock(List<AlgorithmTreeNode> candidateNodes,
            ConversionRuleStep conversionStep) throws BoundError {

        String operationName = extractOperationName(conversionStep.getOperationParam1());
        // We won't extract the field name as it's always the same

        AlgorithmTreeNode executionNode = getNodeWithResult(candidateNodes, operationName);

        return executionNode.getChildren();
    }

    /**
     * @param candidateNodes
     * @param operationToGetFrom
     * @return
     * @throws BoundError
     */
    public static AlgorithmTreeNode getNodeWithResult(List<AlgorithmTreeNode> candidateNodes, String operationName)
            throws BoundError {
        AlgorithmTreeNode executionNode = null;

        for (AlgorithmTreeNode node : candidateNodes) {
            if (operationName.equalsIgnoreCase(node.getAlgorithmRow().getOperation().getValue())) {
                executionNode = node;
            }
        }

        if (executionNode == null) {
            IOpenSourceCodeModule errorSource = candidateNodes.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw new BoundError(String.format("Compilation failure. Can't find %s in operations sequence %s",
                    operationName, candidateNodes), errorSource);
        }
        return executionNode;
    }

    /**
     * @param nodesToCompile
     * @param conversionStep
     * @return
     * @throws BoundError
     */
    public static AlgorithmOperationSource getOperationSource(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep) throws BoundError {

        AlgorithmTreeNode sourceNode;
        String operationValueName = null;

        // TODO: set more precise source reference
        String convertionParam = conversionStep.getOperationParam1();
        if (isOperationFieldInstruction(convertionParam)) {
            sourceNode = getNodeWithResult(nodesToCompile, extractOperationName(convertionParam));
            operationValueName = extractFieldName(convertionParam);
        } else {
            sourceNode = nodesToCompile.get(0);
        }

        return new AlgorithmOperationSource(sourceNode, operationValueName);
    }

    public static StringValue getCellContent(List<AlgorithmTreeNode> candidateNodes, String operationParam)
            throws BoundError {
        String operationName = extractOperationName(operationParam);
        String fieldName = extractFieldName(operationParam);

        AlgorithmTreeNode executionNode = getNodeWithResult(candidateNodes, operationName);

        IOpenField codeField = JavaOpenClass.getOpenClass(AlgorithmRow.class).getField(fieldName);

        if (codeField == null) {
            IOpenSourceCodeModule errorSource = candidateNodes.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw new BoundError(String.format("Compilation failure. Can't find %s field", fieldName), errorSource);
        }

        StringValue openLCode = (StringValue) codeField.get(executionNode.getAlgorithmRow(), null);

        return openLCode;
    }

    public static boolean isOperationFieldInstruction(String instruction) {
        boolean isInstruction = false;

        if (instruction != null) {
            isInstruction = instruction.split(Pattern.quote(FIELD_SEPARATOR)).length == 2;
        }

        return isInstruction;
    }

    public static final String FIELD_SEPARATOR = ".";

    /**
     * @param operationToGetFrom
     */
    public static String extractOperationName(String operationToGetFrom) {
        // Get the first token before ".", it will be the name of operation
        return operationToGetFrom.split(Pattern.quote(FIELD_SEPARATOR))[0];
    }

    /**
     * @param operationToGetFrom
     */
    private static String extractFieldName(String operationToGetFrom) {
        // Get the first token after ".", it will be the field name
        return operationToGetFrom.split(Pattern.quote(FIELD_SEPARATOR))[1];
    }

    public static IOpenSourceCodeModule createSourceCode(List<AlgorithmTreeNode> nodesToCompile, String operationParam)
            throws BoundError {
        StringValue openLCodeValue = getCellContent(nodesToCompile, operationParam);

        return openLCodeValue.asSourceCodeModule();
    }

}
