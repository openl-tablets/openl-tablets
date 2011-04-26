package org.openl.rules.tbasic.compile;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * The <code>AlgoritmNodesCompiler</code> class compiles sequence of nodes
 * inside the specified context.
 *
 */
public class AlgoritmNodesCompiler {
    private LabelManager labelManager;
    private CompileContext currentCompileContext;
    private AlgorithmCompiler compiler;
    
    /** return type for some contexts that are represented as functions**/
    private IOpenClass returnType;

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
        this.compiler = compiler;
        this.returnType = returnType;
    }

    /**
     * after is allowed only for the first operation in group
     *
     * @throws BoundError
     */
    private RuntimeOperation compileAfter(List<AlgorithmTreeNode> nodesToCompile) throws Exception {
        final String afterFieldName = "after";
        return createOperationForFirstNodeField(nodesToCompile, afterFieldName);
    }

    /**
     * before is allowed only for the first operation in group
     *
     * @throws BoundError
     */
    private RuntimeOperation compileBefore(List<AlgorithmTreeNode> nodesToCompile) throws Exception {
        final String beforeFieldName = "before";
        return createOperationForFirstNodeField(nodesToCompile, beforeFieldName);
    }

    private List<RuntimeOperation> compileLinkedNodesGroup(List<AlgorithmTreeNode> nodesToCompile) throws Exception {
        assert nodesToCompile.size() > 0;

        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();

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
        if (!userDefinedLabels.isEmpty() && emittedOperations.size() > 0) {
            for (StringValue userDefinedLabel : userDefinedLabels) {
                currentCompileContext.setLabel(userDefinedLabel.getValue(), emittedOperations.get(0));
            }
        }

        labelManager.finishOperationsSet();

        return emittedOperations;
    }

    private List<RuntimeOperation> compileNestedNodes(List<AlgorithmTreeNode> nodesToProcess) throws Exception {
        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();

        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            if (hasUnreachableCode(nodesToProcess, i)) {
                IOpenSourceCodeModule errorSource = nodesToProcess.get(i + 1).getAlgorithmRow().getOperation()
                        .asSourceCodeModule();
                throw SyntaxNodeExceptionUtils.createError("Unreachable code. Operations after BREAK,CONTINUE not allowed.", errorSource);
            }

            linkedNodesGroupSize = AlgorithmCompilerTool.getLinkedNodesGroupSize(nodesToProcess, i);

            List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);

            emittedOperations.addAll(compileLinkedNodesGroup(nodesToCompile));
        }

        return emittedOperations;
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

    private Object convertParam(List<AlgorithmTreeNode> nodesToCompile, Class<? extends Object> clazz,
            String operationParam) throws SyntaxNodeException {
        // FIXME !!!!

        if (clazz.equals(String.class)) {
            if (labelManager.isLabelInstruction(operationParam)) {
                return labelManager.getLabelByInstruction(operationParam);
            } else if (AlgorithmCompilerTool.isOperationFieldInstruction(operationParam)) {
                StringValue content = AlgorithmCompilerTool.getCellContent(nodesToCompile, operationParam);

                return content.getValue();
            } else {
                // TODO FIXME Do not know how to process
                return operationParam;
            }
        } else if (clazz.equals(boolean.class)) {
            return Boolean.parseBoolean(operationParam);
        } else if (clazz.equals(IMethodCaller.class)) {
            if (operationParam == null) {
                return null;
            } else {
                StringValue cellContent = AlgorithmCompilerTool.getCellContent(nodesToCompile, operationParam);
                IOpenSourceCodeModule src = cellContent.getMetaInfo().getSource();

                AlgorithmTreeNode executionNode = AlgorithmCompilerTool.extractOperationNode(nodesToCompile,
                        operationParam);
                String methodName = operationParam.replace('.', '_') + "_row_"
                        + executionNode.getAlgorithmRow().getRowNumber();
                
                // return statements for the whole Algorithm(TBasic) should be casted to the return type of 
                // whole Algorithm rule
                if (labelManager.isReturnInstruction(operationParam)) {
                    /** create method and cast its value to the appropriate return type*/
                    return compiler.makeMethodWithCast(src, methodName, returnType);
                } else {
                    return compiler.makeMethod(src, methodName);
                }
            }        
        } else {
        
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw SyntaxNodeExceptionUtils.createError(String.format("Compilation failure. Can't convert parameter %s to type %s",
                    operationParam, clazz.toString()), errorSource);
        }
    }

    private RuntimeOperation createOperation(List<AlgorithmTreeNode> nodesToCompile, ConversionRuleStep conversionStep)
            throws Exception {
        try {
            Constructor<?> constructor = getOperationConstructor(conversionStep.getOperationType());

            Object[] params = new Object[constructor.getParameterTypes().length];

            if (constructor.getParameterTypes().length > 0) {
                params[0] = convertParam(nodesToCompile, constructor.getParameterTypes()[0], conversionStep
                        .getOperationParam1());
            }

            if (constructor.getParameterTypes().length > 1) {
                params[1] = convertParam(nodesToCompile, constructor.getParameterTypes()[1], conversionStep
                        .getOperationParam2());
            }

            RuntimeOperation emittedOperation = (RuntimeOperation) constructor.newInstance(params);

            // TODO: set more precise source reference
            AlgorithmOperationSource source = AlgorithmCompilerTool.getOperationSource(nodesToCompile, conversionStep
                    .getOperationParam1());
            emittedOperation.setSourceCode(source);

            String nameForDebug = conversionStep.getNameForDebug();
            emittedOperation.setNameForDebug(nameForDebug);
            emittedOperation.setSignificantForDebug(nameForDebug != null);

            return emittedOperation;

        } catch (Exception e) {
            // IOpenSourceCodeModule errorSource =
            // nodesToCompile.get(0).getAlgorithmRow().getOperation()
            // .asSourceCodeModule();
            // throw new BoundError(e, errorSource);
            throw e;
        }
    }

    private Constructor<?> getOperationConstructor(String operationType) throws ClassNotFoundException {
        Class<?> clazz = Class.forName("org.openl.rules.tbasic.runtime.operations."
                + operationType + "Operation");
        return clazz.getConstructors()[0];
    }

    private RuntimeOperation createOperationForFirstNodeField(List<AlgorithmTreeNode> nodesToCompile, String fieldName)
            throws Exception {
        // TODO: strange method, refactore
        String param = nodesToCompile.get(0).getAlgorithmRow().getOperation() + AlgorithmCompilerTool.FIELD_SEPARATOR
                + fieldName;

        StringValue content = AlgorithmCompilerTool.getCellContent(nodesToCompile, param);
        RuntimeOperation operation = null;

        if (content.getValue() != null && content.getValue().trim() != "") {
            ConversionRuleStep conversionStep = new ConversionRuleStep("Perform", param, null, null, fieldName
                    + " execution");
            operation = createOperation(nodesToCompile, conversionStep);
        }

        return operation;
    }

    private boolean hasUnreachableCode(List<AlgorithmTreeNode> nodesToProcess, int indexOfReturn) {
        if (indexOfReturn < nodesToProcess.size() - 1) {
            if (nodesToProcess.get(indexOfReturn).getSpecification().getKeyword().equals("BREAK")
                    || nodesToProcess.get(indexOfReturn).getSpecification().getKeyword().equals("CONTINUE")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param nodesToCompile
     * @param conversionRule
     * @throws BoundError
     */
    private List<RuntimeOperation> processConversionStep(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep) throws Exception {
        assert nodesToCompile.size() > 0;
        assert conversionStep != null;

        String label = null;
        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();

        // get label for the current step
        if (conversionStep.getLabelInstruction() != null) {
            label = labelManager.getLabelByInstruction(conversionStep.getLabelInstruction());
        }

        String operationType = conversionStep.getOperationType();
        // TODO
        if (!operationType.startsWith("!")) {
            RuntimeOperation emittedOperation = createOperation(nodesToCompile, conversionStep);
            emittedOperations.add(emittedOperation);
        } else if (operationType.equals("!Compile")) {
            List<AlgorithmTreeNode> nodesToProcess;
            nodesToProcess = AlgorithmCompilerTool.getNestedInstructionsBlock(nodesToCompile, conversionStep
                    .getOperationParam1());
            emittedOperations.addAll(compileNestedNodes(nodesToProcess));
        } else if (operationType.equals("!CheckLabel")) {
            String labelName = (String) convertParam(nodesToCompile, String.class, conversionStep.getOperationParam1());
            if (!currentCompileContext.isLabelRegistered(labelName)) {
                IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
                        .asSourceCodeModule();
                throw SyntaxNodeExceptionUtils.createError("Such label is not available from this place: \"" + labelName + "\".", errorSource);
            }
        }
        if (emittedOperations.size() > 0 && label != null) {
            // register internal generated label label
            currentCompileContext.registerNewLabel(label, nodesToCompile.get(0));
            currentCompileContext.setLabel(label, emittedOperations.get(0));
        }

        return emittedOperations;
    }
    
}
