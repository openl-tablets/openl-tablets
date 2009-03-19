package org.openl.rules.tbasic.compile;

import java.util.List;

import org.openl.IOpenSourceCodeModule;
import org.openl.base.INamedThing;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTableParserManager;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class ReturnAnalyzer {
    private IOpenClass returnType;
    private AlgorithmCompiler compiler;

    public ReturnAnalyzer(IOpenClass returnType, AlgorithmCompiler compiler) {
        this.returnType = returnType;
        this.compiler = compiler;
    }

    public IOpenClass getReturnType() {
        return returnType;
    }

    public SuitablityAsReturn analyzeSequence(List<AlgorithmTreeNode> nodesToAnalyze, IOpenClass returnType)
            throws BoundError {
        this.returnType = returnType;
        return analyzeSequence(nodesToAnalyze);
    }

    public SuitablityAsReturn analyzeSequence(List<AlgorithmTreeNode> nodesToAnalyze) throws BoundError {
        SuitablityAsReturn result = SuitablityAsReturn.RETURN;
        if (returnType == JavaOpenClass.VOID) {
            // not requires return
            return result;
        }
        for (int i = 0, linkedNodesGroupSize; i < nodesToAnalyze.size(); i += linkedNodesGroupSize) {
            linkedNodesGroupSize = AlgorithmCompiler.getLinkedNodesGroupSize(nodesToAnalyze, i);
            if (linkedNodesGroupSize == 1) {
                result = analyzeNode(nodesToAnalyze.get(i));
            } else {
                result = analyzeGroup(nodesToAnalyze.subList(i, i + linkedNodesGroupSize));
            }
            if (result == SuitablityAsReturn.RETURN && i + linkedNodesGroupSize < nodesToAnalyze.size()) {
                IOpenSourceCodeModule errorSource = nodesToAnalyze.get(i + linkedNodesGroupSize).getAlgorithmRow()
                        .getOperation().asSourceCodeModule();
                throw new BoundError("Unreachable code. Operations after RETURN not allowed.", errorSource);
            }
        }
        return result;
    }

    private SuitablityAsReturn analyzeIFOperationMultiLine(List<AlgorithmTreeNode> nodesToAnalyze) throws BoundError {
        SuitablityAsReturn result = SuitablityAsReturn.RETURN;
        for (int i = 0; i < nodesToAnalyze.size(); i++) {
            if (nodesToAnalyze.get(i).getChildren().size() > 0) {
                result = SuitablityAsReturn.lessSuitable(result, analyzeSequence(nodesToAnalyze.get(i).getChildren()));
            }
        }
        return result;
    }

    private SuitablityAsReturn analyzeIFOperationSingleLine(List<AlgorithmTreeNode> nodesToAnalyze) throws BoundError {
        SuitablityAsReturn result = SuitablityAsReturn.RETURN;
        for (int i = 0; i < nodesToAnalyze.size(); i++) {
            if (!nodesToAnalyze.get(i).getAlgorithmRow().getAction().equals("")) {
                SuitablityAsReturn suitablityOfNode = SuitablityAsReturn.NONE;
                if (returnType.equals(getTypeOfField(nodesToAnalyze.get(i).getAlgorithmRow().getAction()))) {
                    suitablityOfNode = SuitablityAsReturn.SUITABLE;
                }
                result = SuitablityAsReturn.lessSuitable(result, suitablityOfNode);
            }
        }
        return result;
    }

    private boolean canBeGrouped(AlgorithmTreeNode nodeToAnalyze) {
        String currentNodeKeyword = nodeToAnalyze.getSpecification().getKeyword();
        String[] operationNamesToGroup = AlgorithmTableParserManager.instance().whatOperationsToGroup(
                currentNodeKeyword);
        if (operationNamesToGroup != null) {
            return true;
        } else {
            return false;
        }
    }

    private SuitablityAsReturn analyzeGroup(List<AlgorithmTreeNode> nodesToAnalyze) throws BoundError {
        if (nodesToAnalyze.get(0).getSpecification().getKeyword().equals("IF")
                && nodesToAnalyze.get(1).getSpecification().getKeyword().equals("ELSE")) {
            if (nodesToAnalyze.get(0).getSpecification().isMultiline()) {
                return analyzeIFOperationMultiLine(nodesToAnalyze);
            } else {
                return analyzeIFOperationSingleLine(nodesToAnalyze);
            }
        } else {
            return SuitablityAsReturn.NONE;
        }
    }

    private SuitablityAsReturn analyzeNode(AlgorithmTreeNode nodeToAnalyze) throws BoundError {
        if (nodeToAnalyze.getSpecification().getKeyword().equals("RETURN")) {
            if (returnType.equals(getTypeOfField(nodeToAnalyze.getAlgorithmRow().getCondition()))) {
                return SuitablityAsReturn.RETURN;
            } else {
                IOpenSourceCodeModule errorSource = nodeToAnalyze.getAlgorithmRow().getCondition().asSourceCodeModule();
                throw new BoundError("Incorrect return type. Return type of function declared as '"
                        + returnType.getDisplayName(INamedThing.REGULAR) + "'", errorSource);
            }
        } else if (canBeGrouped(nodeToAnalyze)) {
            // for loops and single IF without ELSE
            return SuitablityAsReturn.NONE;
        } else if (returnType.equals(getTypeOfField(nodeToAnalyze.getAlgorithmRow().getAction()))) {
            return SuitablityAsReturn.SUITABLE;
        } else {
            return SuitablityAsReturn.NONE;
        }
    }

    private IOpenClass getTypeOfField(StringValue fieldContent) {
        return compiler.getTypeOfField(fieldContent);
    }
}
