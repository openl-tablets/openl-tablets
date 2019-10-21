package org.openl.rules.tbasic.compile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmRow;
import org.openl.rules.tbasic.AlgorithmTableParserManager;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class AlgorithmCompilerTool {

    public static final String FIELD_SEPARATOR = ".";

    private static String extractFieldName(String instruction) {
        // Get the first token after ".", it will be the field name
        return instruction.split(Pattern.quote(FIELD_SEPARATOR))[1];
    }

    private static String extractOperationName(String instruction) {
        // Get the first token before ".", it will be the name of operation
        return instruction.split(Pattern.quote(FIELD_SEPARATOR))[0];
    }

    /**
     * @param candidateNodes
     * @param instruction
     * @return The {@link org.openl.rules.tbasic.AlgorithmTreeNode} that suits the given instruction name
     *
     * @throws SyntaxNodeException
     */
    public static AlgorithmTreeNode extractOperationNode(List<AlgorithmTreeNode> candidateNodes,
            String instruction) throws SyntaxNodeException {
        AlgorithmTreeNode operationNode = null;

        // Get the name of the operation: e.g. VAR, IF, WHILE, etc
        //
        String operationName = extractOperationName(instruction);

        for (AlgorithmTreeNode node : candidateNodes) {
            if (isOperationNode(operationName, node)) {
                operationNode = node;
            }
        }

        if (operationNode == null) {
            IOpenSourceCodeModule errorSource = candidateNodes.get(0)
                .getAlgorithmRow()
                .getOperation()
                .asSourceCodeModule();
            throw SyntaxNodeExceptionUtils.createError(String
                .format("Compilation failure. Cannot find %s in operations sequence %s", operationName, candidateNodes),
                errorSource);
        }
        return operationNode;
    }

    private static boolean isOperationNode(String operationName, AlgorithmTreeNode node) {
        return operationName.equalsIgnoreCase(node.getAlgorithmRow().getOperation().getValue());
    }

    public static Map<String, AlgorithmTreeNode> getAllDeclaredLables(List<AlgorithmTreeNode> nodesToSearch) {
        Map<String, AlgorithmTreeNode> labels = new HashMap<>();
        for (AlgorithmTreeNode node : nodesToSearch) {
            for (StringValue labelOfNode : node.getLabels()) {
                labels.put(labelOfNode.getValue(), node);
            }
            labels.putAll(getAllDeclaredLables(node.getChildren()));
        }
        return labels;
    }

    /**
     * @param candidateNodes
     * @param instruction
     * @return
     * @throws SyntaxNodeException
     */
    public static StringValue getCellContent(List<AlgorithmTreeNode> candidateNodes,
            String instruction) throws SyntaxNodeException {
        // Field of the AlgorithmRow.class, that also is the column in the TBasic table
        //
        String fieldName = extractFieldName(instruction);

        IOpenField codeField = JavaOpenClass.getOpenClass(AlgorithmRow.class).getField(fieldName);

        if (codeField == null) {
            IOpenSourceCodeModule errorSource = candidateNodes.get(0)
                .getAlgorithmRow()
                .getOperation()
                .asSourceCodeModule();
            throw SyntaxNodeExceptionUtils
                .createError(String.format("Compilation failure. Cannot find '%s' field", fieldName), errorSource);
        }

        // Get the operation node from the candidate nodes, that suits to the given instruction
        //
        AlgorithmTreeNode executionNode = extractOperationNode(candidateNodes, instruction);

        return (StringValue) codeField.get(executionNode.getAlgorithmRow(), null);
    }

    /**
     *
     * @param nodes
     * @return
     */
    public static AlgorithmTreeNode getLastExecutableOperation(List<AlgorithmTreeNode> nodes) {
        AlgorithmTreeNode lastOperation = nodes.get(nodes.size() - 1);
        if (lastOperation.getSpecificationKeyword().startsWith(TBasicSpecificationKey.END.toString())) {
            lastOperation = getLastExecutableOperation(nodes.subList(0, nodes.size() - 1));
        } else if (!lastOperation.getChildren().isEmpty()) {
            lastOperation = getLastExecutableOperation(lastOperation.getChildren());
        }
        return lastOperation;
    }

    /**
     * @param nodesToProcess
     * @param firstNodeIndex
     * @return
     */
    public static int getLinkedNodesGroupSize(List<AlgorithmTreeNode> nodesToProcess, int firstNodeIndex) {
        int linkedNodesGroupSize = 1; // just one operation by default

        AlgorithmTreeNode currentNodeToProcess = nodesToProcess.get(firstNodeIndex);
        String currentNodeKeyword = currentNodeToProcess.getSpecificationKeyword();

        String[] operationNamesToGroup = AlgorithmTableParserManager.getInstance()
            .whatOperationsToGroup(currentNodeKeyword);

        if (operationNamesToGroup != null) {
            List<String> operationsToGroupWithCurrent = Arrays.asList(operationNamesToGroup);

            for (; linkedNodesGroupSize < nodesToProcess.size() - firstNodeIndex; linkedNodesGroupSize++) {
                AlgorithmTreeNode groupCandidateNode = nodesToProcess.get(firstNodeIndex + linkedNodesGroupSize);
                if (!operationsToGroupWithCurrent.contains(groupCandidateNode.getSpecificationKeyword())) {
                    break;
                }
            }
        }

        return linkedNodesGroupSize;
    }

    /**
     * @param candidateNodes
     * @param conversionStep
     * @return
     */
    public static List<AlgorithmTreeNode> getNestedInstructionsBlock(List<AlgorithmTreeNode> candidateNodes,
            String instruction) throws SyntaxNodeException {

        AlgorithmTreeNode executionNode = extractOperationNode(candidateNodes, instruction);

        return executionNode.getChildren();
    }

    /**
     * @param nodesToCompile
     * @param instruction
     * @return
     */
    public static AlgorithmOperationSource getOperationSource(List<AlgorithmTreeNode> nodesToCompile,
            String instruction) throws SyntaxNodeException {

        AlgorithmTreeNode sourceNode;
        String operationValueName = null;

        // TODO: set more precise source reference
        if (isOperationFieldInstruction(instruction)) {
            sourceNode = extractOperationNode(nodesToCompile, instruction);
            operationValueName = extractFieldName(instruction);
        } else {
            sourceNode = nodesToCompile.get(0);
        }

        return new AlgorithmOperationSource(sourceNode, operationValueName);
    }

    /**
     * @param instruction
     * @return
     */
    public static boolean isOperationFieldInstruction(String instruction) {
        boolean isInstruction = false;

        if (instruction != null) {
            isInstruction = instruction.split(Pattern.quote(FIELD_SEPARATOR)).length == 2;
        }

        return isInstruction;
    }

}
