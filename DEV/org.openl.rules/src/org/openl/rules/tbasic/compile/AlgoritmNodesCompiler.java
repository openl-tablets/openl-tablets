package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.Runnable;
import org.openl.syntax.exception.SyntaxNodeExceptionCollector;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;

/**
 * The <code>AlgoritmNodesCompiler</code> class compiles sequence of nodes
 * inside the specified context.
 *
 */
public class AlgoritmNodesCompiler {
    private LabelManager labelManager;
    private CompileContext currentCompileContext;
    private ParameterConverterManager parameterConverter;
    private OperationFactory operationFactory;

    private List<OperationAnalyzer> operationAnalyzers = new ArrayList<OperationAnalyzer>();

    {
        operationAnalyzers.add(new CommonOperations());
        operationAnalyzers.add(new NotCompileOperations());
        operationAnalyzers.add(new NotCheckLabelOperations());
    }

    /**
     * Create an instance of <code>AlgoritmNodesCompiler</code>.
     *
     * @param returnType Return type for some contexts that are represented as functions
     * @param currentCompileContext Context of compilation of nodes.
     * @param compiler Main algorithm compiler
     */
    public AlgoritmNodesCompiler(IOpenClass returnType, CompileContext currentCompileContext,
            AlgorithmCompiler compiler) {        
        /**Label Manager that controls the labels.**/
        this.labelManager = compiler.getLabelManager();
        this.currentCompileContext = currentCompileContext;
        this.parameterConverter = new ParameterConverterManager(compiler, returnType);
        this.operationFactory = new OperationFactory(parameterConverter);
    }

    /**
     * Compile sequence of nodes.
     *
     * @param nodes Nodes to compile.
     * @return Compiled code.
     *
     * @throws Exception If nodes have errors.
     */
    public List<RuntimeOperation> compileNodes(List<AlgorithmTreeNode> nodes) throws Exception {
        return compileNestedNodes(nodes);
    }

    private List<RuntimeOperation> compileNestedNodes(List<AlgorithmTreeNode> nodesToProcess) throws Exception {
        final List<RuntimeOperation> emittedOperations = new ArrayList<>();
        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            if (hasUnreachableCode(nodesToProcess, i)) {
                IOpenSourceCodeModule errorSource = nodesToProcess.get(i + 1).getAlgorithmRow().getOperation()
                        .asSourceCodeModule();
                throw SyntaxNodeExceptionUtils.createError("Unreachable code. Operations after BREAK,CONTINUE not allowed.", errorSource);
            }

            linkedNodesGroupSize = AlgorithmCompilerTool.getLinkedNodesGroupSize(nodesToProcess, i);

            final List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);
            syntaxNodeExceptionCollector.run(() -> emittedOperations.addAll(compileLinkedNodesGroup(nodesToCompile)));
        }
        
        syntaxNodeExceptionCollector.throwIfAny("Compilation fails!");

        return emittedOperations;
    }

    private List<RuntimeOperation> compileLinkedNodesGroup(List<AlgorithmTreeNode> nodesToCompile) throws Exception {
        assert nodesToCompile.size() > 0;

        List<RuntimeOperation> emittedOperations = new ArrayList<>();

        ConversionRuleBean conversionRule = ConversionRulesController.getInstance().getConvertionRule(nodesToCompile);

        // the first operation always contains definition
        boolean isLoopOperation = nodesToCompile.get(0).getSpecification().isLoopOperation();
        labelManager.startOperationsSet(isLoopOperation);

        labelManager.generateAllLabels(conversionRule.getLabel());

        // compile before statement
        RuntimeOperation beforeOperation = compileBefore(nodesToCompile);
        if (beforeOperation != null) {
            emittedOperations.add(beforeOperation);
        }

        for (ConversionRuleStep convertionStep : conversionRule.getConvertionSteps()) {
            List<RuntimeOperation> stepEmittedOperations = processConversionStep(nodesToCompile, convertionStep);
            emittedOperations.addAll(stepEmittedOperations);
        }

        // compile after statement
        RuntimeOperation afterOperation = compileAfter(nodesToCompile);
        if (afterOperation != null) {
            emittedOperations.add(afterOperation);
        }

        // apply user defined label to the first emitted operation
        // label can be defined only for the first operation in the group
        List<StringValue> userDefinedLabels = nodesToCompile.get(0).getLabels();
        if (!userDefinedLabels.isEmpty() && !emittedOperations.isEmpty()) {
            for (StringValue userDefinedLabel : userDefinedLabels) {
                currentCompileContext.setLabel(userDefinedLabel.getValue(), emittedOperations.get(0));
            }
        }

        labelManager.finishOperationsSet();

        return emittedOperations;
    }

    /**
     * after is allowed only for the first operation in group
     *
     */
    private RuntimeOperation compileAfter(List<AlgorithmTreeNode> nodesToCompile) throws Exception {
        final String afterFieldName = "after";
        return createOperationForFirstNodeField(nodesToCompile, afterFieldName);
    }

    /**
     * before is allowed only for the first operation in group
     *
     */
    private RuntimeOperation compileBefore(List<AlgorithmTreeNode> nodesToCompile) throws Exception {
        final String beforeFieldName = "before";
        return createOperationForFirstNodeField(nodesToCompile, beforeFieldName);
    }

    private RuntimeOperation createOperationForFirstNodeField(List<AlgorithmTreeNode> nodesToCompile, String fieldName)
            throws Exception {
        // TODO: strange method, refactore
        String param = nodesToCompile.get(0).getAlgorithmRow().getOperation() + AlgorithmCompilerTool.FIELD_SEPARATOR
                + fieldName;

        StringValue content = AlgorithmCompilerTool.getCellContent(nodesToCompile, param);
        RuntimeOperation operation = null;

        if (content.getValue() != null && !content.getValue().trim().isEmpty()) {
            ConversionRuleStep conversionStep = new ConversionRuleStep("Perform", param, null, null, fieldName
                    + " execution");
            operation = operationFactory.createOperation(nodesToCompile, conversionStep);
        }

        return operation;
    }

    private boolean hasUnreachableCode(List<AlgorithmTreeNode> nodesToProcess, int indexOfReturn) {
        if (indexOfReturn < nodesToProcess.size() - 1) {
            if (TBasicSpecificationKey.BREAK.toString().equals(nodesToProcess.get(indexOfReturn).getSpecificationKeyword())
                    || TBasicSpecificationKey.CONTINUE.toString().equals(nodesToProcess.get(indexOfReturn).getSpecificationKeyword())) {
                return true;
            }
        }
        return false;
    }

    private List<RuntimeOperation> processConversionStep(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep) throws Exception {
        assert !nodesToCompile.isEmpty();
        assert conversionStep != null;

        String label = null;


        // get label for the current step
        if (conversionStep.getLabelInstruction() != null) {
            label = labelManager.getLabelByInstruction(conversionStep.getLabelInstruction());
        }

        String operationType = conversionStep.getOperationType();
        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();
        for (OperationAnalyzer analyzer : operationAnalyzers) {
            if (analyzer.suits(operationType)) {
                List<RuntimeOperation> operations = analyzer.getOperations(nodesToCompile, conversionStep);
                if (operations != null) {
                    emittedOperations.addAll(operations);
                }

            }
        }

        if (!emittedOperations.isEmpty() && label != null) {
            // register internal generated label label
            currentCompileContext.registerNewLabel(label, nodesToCompile.get(0));
            currentCompileContext.setLabel(label, emittedOperations.get(0));
        }

        return emittedOperations;
    }

    public interface OperationAnalyzer {
        boolean suits(String operationType);
        List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                                             ConversionRuleStep conversionStep) throws Exception;
    }

    private final class CommonOperations implements OperationAnalyzer {

        @Override
        public boolean suits(String operationType) {
            return !operationType.startsWith("!");
        }

        @Override
        public List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                                                    ConversionRuleStep conversionStep) throws Exception {
            List<RuntimeOperation> emittedOperations = new ArrayList<>();
            RuntimeOperation emittedOperation = operationFactory.createOperation(nodesToCompile, conversionStep);
            emittedOperations.add(emittedOperation);
            return emittedOperations;
        }
    }

    private final class NotCompileOperations implements OperationAnalyzer {

        @Override
        public boolean suits(String operationType) {
            return operationType.equals(OperationType.COMPILE.toString());
        }

        @Override
        public List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                                                    ConversionRuleStep conversionStep) throws Exception {
            List<RuntimeOperation> emittedOperations = new ArrayList<>();
            List<AlgorithmTreeNode> nodesToProcess;
            nodesToProcess = AlgorithmCompilerTool.getNestedInstructionsBlock(nodesToCompile, conversionStep
                    .getOperationParam1());
            emittedOperations.addAll(compileNestedNodes(nodesToProcess));
            return emittedOperations;
        }
    }

    private final class NotCheckLabelOperations implements OperationAnalyzer {

        @Override
        public boolean suits(String operationType) {
            return operationType.equals(OperationType.CHECK_LABEL.toString());
        }

        @Override
        public List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                                                    ConversionRuleStep conversionStep) throws Exception {
            String labelName = (String) parameterConverter.convertParam(nodesToCompile, String.class,
                    conversionStep.getOperationParam1());
            if (!currentCompileContext.isLabelRegistered(labelName)) {
                IOpenSourceCodeModule errorSource =
                        nodesToCompile.get(0).getAlgorithmRow().getOperation().asSourceCodeModule();
                String errorMessage = String.format("Such label is not available from this place: \"%s\".", labelName);
                throw SyntaxNodeExceptionUtils.createError(errorMessage, errorSource);
            }
            return null;
        }
    }


}
