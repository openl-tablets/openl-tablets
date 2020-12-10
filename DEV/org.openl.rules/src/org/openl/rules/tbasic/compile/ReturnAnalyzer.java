package org.openl.rules.tbasic.compile;

import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTableParserManager;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * The <code>ReturnAnalyzer</code> class analyzes body of some TBasic function for correctness of returns sequence and
 * return types and detects unreachable code.
 *
 */
public class ReturnAnalyzer {
    private final IOpenClass returnType;
    private final AlgorithmCompiler compiler;

    /**
     * Create an instance of <code>ReturnAnalyzer</code> for analysis of some function from TBasic compiler.
     *
     * @param returnType Expected return type of function
     * @param compiler Associated TBasic compiler.
     */
    public ReturnAnalyzer(IOpenClass returnType, AlgorithmCompiler compiler) {
        this.returnType = returnType;
        this.compiler = compiler;
    }

    /**
     * Make full analysis of correctness of returns sequence and return types and detects unreachable code.
     *
     * @param nodesToAnalyze Body of some function to analyze.
     * @return Correctness of code.
     * @throws SyntaxNodeException If function contains unreachable code or incorrect return type.
     */
    public SuitablityAsReturn analyze(List<AlgorithmTreeNode> nodesToAnalyze, IBindingContext bindingContext) {
        if (returnType != JavaOpenClass.VOID) {
            return analyzeSequence(nodesToAnalyze, bindingContext);
        }
        // not requires result
        return SuitablityAsReturn.RETURN;
    }

    private SuitablityAsReturn analyzeIFOperation(List<AlgorithmTreeNode> nodesToAnalyze,
            boolean isMultiline,
            IBindingContext bindingContext) {
        SuitablityAsReturn result = SuitablityAsReturn.RETURN;
        // checks only IF and ELSE branches
        for (int i = 0; i < 2; i++) {
            SuitablityAsReturn suitablityOfNode;
            if (isMultiline) {
                suitablityOfNode = analyzeSequence(nodesToAnalyze.get(i).getChildren(), bindingContext);
            } else {
                if (hasTypeAsReturn(nodesToAnalyze.get(i).getAlgorithmRow().getAction(), bindingContext)) {
                    suitablityOfNode = SuitablityAsReturn.SUITABLE;
                } else {
                    suitablityOfNode = SuitablityAsReturn.NONE;
                }
            }
            result = SuitablityAsReturn.lessSuitable(result, suitablityOfNode);
        }
        return result;
    }

    private SuitablityAsReturn analyzeNode(AlgorithmTreeNode nodeToAnalyze, IBindingContext bindingContext) {
        if (TBasicSpecificationKey.RETURN.toString().equals(nodeToAnalyze.getSpecificationKeyword())) {
            if (hasTypeAsReturn(nodeToAnalyze.getAlgorithmRow().getCondition(), bindingContext)) {
                return SuitablityAsReturn.RETURN;
            } else {
                IOpenSourceCodeModule errorSource = nodeToAnalyze.getAlgorithmRow().getCondition().asSourceCodeModule();
                BindHelper
                    .processError(
                        "Incorrect return type. Return type of function declared as '" + returnType
                            .getDisplayName(INamedThing.REGULAR) + "'",
                        errorSource,
                        bindingContext);
                return SuitablityAsReturn.NONE;
            }
        } else if (canBeGrouped(nodeToAnalyze)) {
            // for loops and single IF without ELSE
            return SuitablityAsReturn.NONE;
        } else if (hasTypeAsReturn(nodeToAnalyze.getAlgorithmRow().getAction(), bindingContext)) {
            return SuitablityAsReturn.SUITABLE;
        } else {
            return SuitablityAsReturn.NONE;
        }
    }

    private SuitablityAsReturn analyzeSequence(List<AlgorithmTreeNode> nodesToAnalyze, IBindingContext bindingContext) {
        SuitablityAsReturn result = SuitablityAsReturn.RETURN;
        for (int i = 0, linkedNodesGroupSize; i < nodesToAnalyze.size(); i += linkedNodesGroupSize) {
            linkedNodesGroupSize = AlgorithmCompilerTool.getLinkedNodesGroupSize(nodesToAnalyze, i);

            if (linkedNodesGroupSize == 1) {
                result = analyzeNode(nodesToAnalyze.get(i), bindingContext);
            } else {
                List<AlgorithmTreeNode> nodesToAnalyze1 = nodesToAnalyze.subList(i, i + linkedNodesGroupSize);
                SuitablityAsReturn result1;
                if (TBasicSpecificationKey.IF.toString()
                    .equals(nodesToAnalyze1.get(0).getSpecificationKeyword()) && TBasicSpecificationKey.ELSE.toString()
                        .equals(nodesToAnalyze1.get(1).getSpecificationKeyword())) {
                    result1 = analyzeIFOperation(nodesToAnalyze1,
                        nodesToAnalyze1.get(0).getSpecification().isMultiline(),
                        bindingContext);
                } else {
                    result1 = SuitablityAsReturn.NONE;
                }
                result = result1;
            }

            if (result == SuitablityAsReturn.RETURN && i + linkedNodesGroupSize < nodesToAnalyze.size()) {
                IOpenSourceCodeModule errorSource = nodesToAnalyze.get(i + linkedNodesGroupSize)
                    .getAlgorithmRow()
                    .getOperation()
                    .asSourceCodeModule();
                BindHelper.processError("Unreachable code. Operations after RETURN not allowed.",
                    errorSource,
                    bindingContext);
            }
        }
        return result;
    }

    private boolean canBeGrouped(AlgorithmTreeNode nodeToAnalyze) {
        String currentNodeKeyword = nodeToAnalyze.getSpecificationKeyword();
        String[] operationNamesToGroup = AlgorithmTableParserManager.getInstance()
            .whatOperationsToGroup(currentNodeKeyword);
        return operationNamesToGroup != null;
    }

    /**
     *
     * @return Expected return type of function
     */
    public IOpenClass getReturnType() {
        return returnType;
    }

    private boolean hasTypeAsReturn(StringValue fieldContent, IBindingContext bindingContext) {
        if (returnType == JavaOpenClass.VOID) {
            // for void functions return must be empty
            return fieldContent.getValue().equals("");
        }
        IOpenClass typeOfField = compiler.getTypeOfField(fieldContent, bindingContext);
        return returnType.equals(typeOfField);
    }
}
