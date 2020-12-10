package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenClass;

/**
 * The <code>AlgoritmNodesCompiler</code> class compiles sequence of nodes inside the specified context.
 *
 */
public class AlgoritmNodesCompiler {
    private final LabelManager labelManager;
    private final CompileContext currentCompileContext;
    private final ParameterConverterManager parameterConverter;
    private final OperationFactory operationFactory;

    private final List<OperationAnalyzer> operationAnalyzers = new ArrayList<>();

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
    public AlgoritmNodesCompiler(IOpenClass returnType,
            CompileContext currentCompileContext,
            AlgorithmCompiler compiler) {
        /** Label Manager that controls the labels. **/
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
    public List<RuntimeOperation> compileNodes(List<AlgorithmTreeNode> nodes, IBindingContext bindingContext) {
        return compileNestedNodes(nodes, bindingContext);
    }

    private List<RuntimeOperation> compileNestedNodes(List<AlgorithmTreeNode> nodesToProcess,
            IBindingContext bindingContext) {
        final List<RuntimeOperation> emittedOperations = new ArrayList<>();
        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            if (hasUnreachableCode(nodesToProcess, i)) {
                IOpenSourceCodeModule errorSource = nodesToProcess.get(i + 1)
                    .getAlgorithmRow()
                    .getOperation()
                    .asSourceCodeModule();
                BindHelper.processError("Unreachable code. Operations after BREAK,CONTINUE not allowed.",
                    errorSource,
                    bindingContext);
            }

            linkedNodesGroupSize = AlgorithmCompilerTool.getLinkedNodesGroupSize(nodesToProcess, i);

            final List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);
            emittedOperations.addAll(compileLinkedNodesGroup(nodesToCompile, bindingContext));
        }

        return emittedOperations;
    }

    private List<RuntimeOperation> compileLinkedNodesGroup(List<AlgorithmTreeNode> nodesToCompile,
            IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();

        List<RuntimeOperation> emittedOperations = new ArrayList<>();

        ConversionRuleBean conversionRule = ConversionRulesController.getInstance()
            .getConvertionRule(nodesToCompile, bindingContext);
        if (conversionRule == null) {
            return Collections.emptyList();
        }

        // the first operation always contains definition
        boolean isLoopOperation = nodesToCompile.get(0).getSpecification().isLoopOperation();
        labelManager.startOperationsSet(isLoopOperation);

        labelManager.generateAllLabels(conversionRule.getLabel());

        // compile before statement
        RuntimeOperation beforeOperation = createOperationForFirstNodeField(nodesToCompile, "before", bindingContext);
        if (beforeOperation != null) {
            emittedOperations.add(beforeOperation);
        }

        for (ConversionRuleStep convertionStep : conversionRule.getConvertionSteps()) {
            List<RuntimeOperation> stepEmittedOperations = processConversionStep(nodesToCompile,
                convertionStep,
                bindingContext);
            emittedOperations.addAll(stepEmittedOperations);
        }

        // compile after statement
        RuntimeOperation afterOperation = createOperationForFirstNodeField(nodesToCompile, "after", bindingContext);
        if (afterOperation != null) {
            emittedOperations.add(afterOperation);
        }

        // apply user defined label to the first emitted operation
        // label can be defined only for the first operation in the group
        List<StringValue> userDefinedLabels = nodesToCompile.get(0).getLabels();
        if (!userDefinedLabels.isEmpty() && !emittedOperations.isEmpty()) {
            for (StringValue userDefinedLabel : userDefinedLabels) {
                currentCompileContext.setLabel(userDefinedLabel.getValue(), emittedOperations.get(0), bindingContext);
            }
        }

        labelManager.finishOperationsSet();

        return emittedOperations;
    }

    private RuntimeOperation createOperationForFirstNodeField(List<AlgorithmTreeNode> nodesToCompile,
            String fieldName,
            IBindingContext bindingContext) {
        // TODO: strange method, refactore
        String param = nodesToCompile.get(0)
            .getAlgorithmRow()
            .getOperation() + AlgorithmCompilerTool.FIELD_SEPARATOR + fieldName;

        StringValue content = AlgorithmCompilerTool.getCellContent(nodesToCompile, param, bindingContext);
        RuntimeOperation operation = null;

        if (content.getValue() != null && !content.getValue().trim().isEmpty()) {
            ConversionRuleStep conversionStep = new ConversionRuleStep("Perform",
                param,
                null,
                null,
                fieldName + " execution");
            operation = operationFactory.createOperation(nodesToCompile, conversionStep, bindingContext);
        }

        return operation;
    }

    private static boolean hasUnreachableCode(List<AlgorithmTreeNode> nodesToProcess, int indexOfReturn) {
        return indexOfReturn < nodesToProcess.size() - 1
                && (TBasicSpecificationKey.BREAK.toString()
                        .equals(nodesToProcess.get(indexOfReturn).getSpecificationKeyword())
                    || TBasicSpecificationKey.CONTINUE.toString()
                        .equals(nodesToProcess.get(indexOfReturn).getSpecificationKeyword()));
    }

    private List<RuntimeOperation> processConversionStep(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep,
            IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();
        assert conversionStep != null;

        String label = null;

        // get label for the current step
        if (conversionStep.getLabelInstruction() != null) {
            label = labelManager.getLabelByInstruction(conversionStep.getLabelInstruction());
        }

        String operationType = conversionStep.getOperationType();
        List<RuntimeOperation> emittedOperations = new ArrayList<>();
        for (OperationAnalyzer analyzer : operationAnalyzers) {
            if (analyzer.suits(operationType)) {
                List<RuntimeOperation> operations = analyzer
                    .getOperations(nodesToCompile, conversionStep, bindingContext);
                if (operations != null) {
                    emittedOperations.addAll(operations);
                }

            }
        }

        if (!emittedOperations.isEmpty() && label != null) {
            // register internal generated label label
            currentCompileContext.registerNewLabel(label, nodesToCompile.get(0), bindingContext);
            currentCompileContext.setLabel(label, emittedOperations.get(0), bindingContext);
        }

        return emittedOperations;
    }

    public interface OperationAnalyzer {
        boolean suits(String operationType);

        List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext);
    }

    private final class CommonOperations implements OperationAnalyzer {

        @Override
        public boolean suits(String operationType) {
            return !operationType.startsWith("!");
        }

        @Override
        public List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            List<RuntimeOperation> emittedOperations = new ArrayList<>();
            RuntimeOperation emittedOperation = operationFactory
                .createOperation(nodesToCompile, conversionStep, bindingContext);
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
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            List<AlgorithmTreeNode> nodesToProcess;
            nodesToProcess = AlgorithmCompilerTool
                .getNestedInstructionsBlock(nodesToCompile, conversionStep.getOperationParam1(), bindingContext);
            return new ArrayList<>(compileNestedNodes(nodesToProcess,
                    bindingContext));
        }
    }

    private final class NotCheckLabelOperations implements OperationAnalyzer {

        @Override
        public boolean suits(String operationType) {
            return operationType.equals(OperationType.CHECK_LABEL.toString());
        }

        @Override
        public List<RuntimeOperation> getOperations(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            String labelName = (String) parameterConverter
                .convertParam(nodesToCompile, String.class, conversionStep.getOperationParam1(), bindingContext);
            if (!currentCompileContext.isLabelRegistered(labelName)) {
                IOpenSourceCodeModule errorSource = nodesToCompile.get(0)
                    .getAlgorithmRow()
                    .getOperation()
                    .asSourceCodeModule();
                String errorMessage = String.format("Such label is not available from this place: '%s'.", labelName);
                BindHelper.processError(errorMessage, errorSource, bindingContext);
            }
            return null;
        }
    }

}
