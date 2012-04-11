/**
 * 
 */
package org.openl.rules.tbasic.compile;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmRow;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.NoParamMethodField;
import org.openl.rules.tbasic.AlgorithmTableParserManager;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

// FIXME: !!!!!!!!!!!!!!! refactor to eliminate code duplications and to isolate different functionality in separate classes

/**
 * @author User
 * 
 */
public class AlgorithmCompiler {
    /********************************
     * Initial data
     *******************************/
    private IBindingContext context;
    private IOpenMethodHeader header;
    private List<AlgorithmTreeNode> nodesToCompile;

    /********************************
     * Intermediate values
     *******************************/
    private ConversionRuleBean[] conversionRules;
    private LabelManager labelManager;

    private CompileContext currentCompileContext;

    /*********************************
     * Compiler output
     ********************************/
    private ModuleOpenClass thisTargetClass;
    private CompileContext mainCompileContext;
    private Map<String, CompileContext> internalMethodsContexts;

    public IOpenClass getThisTargetClass() {
        return thisTargetClass;
    }

    public CompileContext getMainCompileContext() {
        return mainCompileContext;
    }

    public Map<String, CompileContext> getInternalMethodsContexts() {
        return internalMethodsContexts;
    }

    public AlgorithmCompiler(IBindingContext context, IOpenMethodHeader header, List<AlgorithmTreeNode> nodesToCompile) {
        this.context = context;
        this.header = header;
        this.nodesToCompile = nodesToCompile;
    }

    /**********************************************
     * Main logic
     **********************************************/

    public void compile(Algorithm algorithm) throws BoundError {
        conversionRules = AlgorithmTableParserManager.instance().getFixedConversionRules();
        labelManager = new LabelManager();
        mainCompileContext = new CompileContext();
        internalMethodsContexts = new HashMap<String, CompileContext>();
        thisTargetClass = new ModuleOpenClass(null, generateOpenClassName(), context.getOpenL());

        precompile(nodesToCompile);
        compile(nodesToCompile);

        // TODO: not very good that we receive 2 separate collections with the
        // same items:
        // operations - list of all operations to execute, labels - register of
        // (label, operation) (operation is the same as in operations)
        algorithm.setThisClass(getThisTargetClass());
        algorithm.setAlgorithmSteps(getMainCompileContext().getOperations());
        algorithm.setLabels(getMainCompileContext().getLocalLabelsRegister());
        processMethodsAfterCompile(algorithm);
    }
    
    private void processMethodsAfterCompile(Algorithm algorithm){
        Iterator<IOpenMethod> openMethods = thisTargetClass.methods();
        for (; openMethods.hasNext();){
            IOpenMethod method = openMethods.next();
            if (method instanceof AlgorithmSubroutineMethod){
                AlgorithmSubroutineMethod algorithmSubroutineMethod = (AlgorithmSubroutineMethod) method;
                CompileContext methodContext = getInternalMethodsContexts().get(algorithmSubroutineMethod.getName());
                
                algorithmSubroutineMethod.setAlgorithmSteps(methodContext.getOperations());
                algorithmSubroutineMethod.setLabels(methodContext.getLocalLabelsRegister());
            }
        }
    }

    /**********************************************
     * Main precompile logic
     **********************************************/

    private void precompile(List<AlgorithmTreeNode> nodesToProcess) throws BoundError {
        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            linkedNodesGroupSize = getLinkedNodesGroupSize(nodesToProcess, i);

            List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);

            precompileLinkedNodesGroup(nodesToCompile);
        }
    }

    private void precompileNestedNodes(List<AlgorithmTreeNode> nodesToProcess) throws BoundError {
        precompile(nodesToProcess);
    }

    void precompileLinkedNodesGroup(List<AlgorithmTreeNode> nodesToCompile) throws BoundError {
        assert nodesToCompile.size() > 0;

        ConversionRuleBean conversionRule = getConvertionRule(nodesToCompile);

        // compile before statement

        for (ConversionRuleStep convertionStep : conversionRule.getConvertionSteps()) {
            preprocessConversionStep(nodesToCompile, convertionStep);
        }
    }

    /*********************************************
     * Single conversion step precompilation logic
     ********************************************/

    /**
     * @param nodesToCompile
     * @param conversionRule
     * @throws BoundError
     */
    private void preprocessConversionStep(List<AlgorithmTreeNode> nodesToCompile, ConversionRuleStep conversionStep)
            throws BoundError {
        assert nodesToCompile.size() > 0;
        assert conversionStep != null;

        String operationType = conversionStep.getOperationType();
        // TODO
        if (!operationType.startsWith("!")) {
            // do nothing
        } else if (operationType.equals("!Compile")) {
            List<AlgorithmTreeNode> nodesToProcess;
            nodesToProcess = getNestedInstructionsBlock(nodesToCompile, conversionStep);
            precompileNestedNodes(nodesToProcess);
        } else if (operationType.equals("!Declare")) {
            declareVariable(nodesToCompile, conversionStep);
        } else if (operationType.equals("!Subroutine")) {
            declareSubroutine(nodesToCompile);
        } else if (operationType.equals("!Function")) {
            declareFunction(nodesToCompile, conversionStep);
        } else {
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw new BoundError(String.format("Unknown compilation instruction %s", operationType), errorSource);
        }
    }

    private void declareSubroutine(List<AlgorithmTreeNode> nodesToCompile) throws BoundError {
        createAlgorithmInternalMethod(nodesToCompile, JavaOpenClass.VOID);

//            IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
//            .asSourceCodeModule();
//            throw new BoundError(String.format("Can't compile subroutine %s", methodName), errorSource);
    }
    
    private void declareFunction(List<AlgorithmTreeNode> nodesToCompile, ConversionRuleStep convertionStep) throws BoundError {
        String returnValueInstruction = convertionStep.getOperationParam1();
        
        IOpenClass returnType = JavaOpenClass.VOID;
        if (isOperationFieldInstruction(returnValueInstruction)){
            returnType = getTypeOfFieldValue(nodesToCompile, returnValueInstruction);
        } else {
            // TODO add support of specification instruction
            returnType = discoverFunctionType(nodesToCompile.get(0).getChildren(), returnValueInstruction);
        }
        createAlgorithmInternalMethod(nodesToCompile, returnType);
        
    }
    
    private IOpenClass discoverFunctionType(List<AlgorithmTreeNode> children, String returnValueInstruction) throws BoundError {
        // FIXME Extremely ugly method (add exceptions, rewrite to be "proper" method, etc.)
        
        // find first RETURN operation
        List<AlgorithmTreeNode> returnNodes = findFirstReturn(children);
        
        assert returnNodes.size() > 0;
        
        //get RETURN.condition part of instruction
        String fieldWithOpenLStatement = "RETURN.condition"; //returnValueInstruction
        
        return getTypeOfFieldValue(returnNodes, fieldWithOpenLStatement);        
    }

    private List<AlgorithmTreeNode> findFirstReturn(List<AlgorithmTreeNode> nodes) {
        // FIXME delete this method at all
        List<AlgorithmTreeNode> returnNodeSubList = null;
        for (int i = 0; i < nodes.size() && returnNodeSubList == null; i++){
            if (nodes.get(i).getSpecification().getKeyword().equals("RETURN")){
                returnNodeSubList = nodes.subList(i, i + 1);
            } else if (nodes.get(i).getChildren() != null){
                returnNodeSubList = findFirstReturn(nodes.get(i).getChildren());
            }
        }
        return returnNodeSubList;
    }

    private void createAlgorithmInternalMethod(List<AlgorithmTreeNode> nodesToCompile, IOpenClass returnType){
        // method name will be at every label
        CompileContext methodContext = new CompileContext();
        for (StringValue label : nodesToCompile.get(0).getLabels()){
            String methodName = label.getValue();
            
            IOpenMethodHeader methodHeader = new OpenMethodHeader(methodName, returnType, IMethodSignature.VOID, thisTargetClass);
            
            AlgorithmSubroutineMethod method = new AlgorithmSubroutineMethod(methodHeader);
            
            thisTargetClass.addMethod(method);
            
            // to support parameters free call
            NoParamMethodField methodAlternative = new NoParamMethodField(methodName, method);
            thisTargetClass.addField(methodAlternative);
            
            internalMethodsContexts.put(methodName, methodContext);
        }
    }

    /**********************************************
     * Main compile logic
     **********************************************/

    private void compile(List<AlgorithmTreeNode> nodesToProcess) throws BoundError {

        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            // switching context to the main algorithm, each operation which
            // should have its own context will produce it and switch to it in
            // the first step
            currentCompileContext = mainCompileContext;

            linkedNodesGroupSize = getLinkedNodesGroupSize(nodesToProcess, i);

            List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);

            List<RuntimeOperation> emittedOperations = compileLinkedNodesGroup(nodesToCompile);

            currentCompileContext.getOperations().addAll(emittedOperations);
        }
    }

    private List<RuntimeOperation> compileNestedNodes(List<AlgorithmTreeNode> nodesToProcess) throws BoundError {
        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();

        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            linkedNodesGroupSize = getLinkedNodesGroupSize(nodesToProcess, i);

            List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);

            emittedOperations.addAll(compileLinkedNodesGroup(nodesToCompile));
        }

        return emittedOperations;
    }

    List<RuntimeOperation> compileLinkedNodesGroup(List<AlgorithmTreeNode> nodesToCompile) throws BoundError {
        assert nodesToCompile.size() > 0;

        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();

        ConversionRuleBean conversionRule = getConvertionRule(nodesToCompile);

        // the first operation always contains definition
        boolean isLoopOperation = nodesToCompile.get(0).getSpecification().isLoopOperation();
        labelManager.startOperationsSet(isLoopOperation);

        labelManager.generateAllLabels(conversionRule.getLabel());

        // compile before statement
        RuntimeOperation beforeOperation = compileBefore(nodesToCompile);
        if (beforeOperation != null){
            emittedOperations.add(beforeOperation);
        }

        for (ConversionRuleStep convertionStep : conversionRule.getConvertionSteps()) {
            List<RuntimeOperation> stepEmittedOperations = processConversionStep(nodesToCompile, convertionStep);
            emittedOperations.addAll(stepEmittedOperations);
        }

        // compile after statement
        RuntimeOperation afterOperation = compileAfter(nodesToCompile);
        if (afterOperation != null){
            emittedOperations.add(afterOperation);
        }

        // apply user defined label to the first emitted operation
        // label can be defined only for the first operation in the group
        StringValue[] userDefinedLabels = nodesToCompile.get(0).getLabels();
        if (userDefinedLabels.length > 0 && emittedOperations.size() > 0) {
            for (StringValue userDefinedLabel : userDefinedLabels) {
                currentCompileContext.getLocalLabelsRegister().put(userDefinedLabel.getValue(),
                        emittedOperations.get(0));
            }
        }

        labelManager.finishOperationsSet();

        return emittedOperations;
    }

    /**
     * before is allowed only for the first operation in group
     * 
     * @throws BoundError
     */
    private RuntimeOperation compileBefore(List<AlgorithmTreeNode> nodesToCompile) throws BoundError {
        final String beforeFieldName = "before";
        return createOperationForFirstNodeField(nodesToCompile, beforeFieldName);
    }

    /**
     * after is allowed only for the first operation in group
     * 
     * @throws BoundError
     */
    private RuntimeOperation compileAfter(List<AlgorithmTreeNode> nodesToCompile) throws BoundError {
        final String afterFieldName = "after";
        return createOperationForFirstNodeField(nodesToCompile, afterFieldName);
    }

    private RuntimeOperation createOperationForFirstNodeField(List<AlgorithmTreeNode> nodesToCompile,
            String beforeFieldName) throws BoundError {
        // TODO: strange method, refactore
        String param = nodesToCompile.get(0).getAlgorithmRow().getOperation() + FIELD_SEPARATOR + beforeFieldName;

        StringValue content = getCellContent(nodesToCompile, param);
        RuntimeOperation operation = null;
        
        if (content.getValue() != null && content.getValue().trim() != "") {
            ConversionRuleStep conversionStep = new ConversionRuleStep("Perform", param, null, null);
            operation = createOperation(nodesToCompile, conversionStep);
        }
        
        return operation;
    }

    /*********************************************
     * Single conversion step compilation logic
     ********************************************/

    /**
     * @param nodesToCompile
     * @param conversionRule
     * @throws BoundError
     */
    private List<RuntimeOperation> processConversionStep(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep) throws BoundError {
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
            nodesToProcess = getNestedInstructionsBlock(nodesToCompile, conversionStep);
            emittedOperations.addAll(compileNestedNodes(nodesToProcess));
        } else if (operationType.equals("!Declare")) {
            // do nothing
//            declareVariable(nodesToCompile, conversionStep);
        } else if (operationType.equals("!Subroutine") || operationType.equals("!Function")) {
            // subroutine or function was declared while pre-compilation
            // switch context to subroutine or function
            switchToSubroutineOrFunctionContext(nodesToCompile);
        } else {
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw new BoundError(String.format("Unknown compilation instruction %s", operationType), errorSource);
        }

        // apply label for the current step
        if (emittedOperations.size() > 0 && label != null) {
            currentCompileContext.getLocalLabelsRegister().put(label, emittedOperations.get(0));
        }

        return emittedOperations;
    }

    private void switchToSubroutineOrFunctionContext(List<AlgorithmTreeNode> nodesToCompile) {
        // method name will be at least as the first label
        String methodName = nodesToCompile.get(0).getLabels()[0].getValue();

        CompileContext subroutineCompileContext = internalMethodsContexts.get(methodName);

        currentCompileContext = subroutineCompileContext;
    }

    private RuntimeOperation createOperation(List<AlgorithmTreeNode> nodesToCompile, ConversionRuleStep conversionStep)
            throws BoundError {
        try {
            Class clazz = Class.forName("org.openl.rules.tbasic.runtime.operations." + conversionStep.getOperationType()
                    + "Operation");
            Constructor constructor = clazz.getConstructors()[0];

            Object[] params = new Object[constructor.getParameterTypes().length];

            if (constructor.getParameterTypes().length > 0) {
                params[0] = convertParam(nodesToCompile, constructor.getParameterTypes()[0], conversionStep
                        .getOperationParam1());
            }

            if (constructor.getParameterTypes().length > 1) {
                params[1] = convertParam(nodesToCompile, constructor.getParameterTypes()[1], conversionStep
                        .getOperationParam2());
            }

            // FIXME put source reference

            return (RuntimeOperation) constructor.newInstance(params);

        } catch (Exception e) {
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw new BoundError(e, errorSource);
        }
    }

    private Object convertParam(List<AlgorithmTreeNode> nodesToCompile, Class<? extends Object> clazz,
            String operationParam) throws BoundError {
        // FIXME !!!!

        if (clazz.equals(String.class)) {
            if (labelManager.isLabelInstruction(operationParam)) {
                return labelManager.getLabelByInstruction(operationParam);
            } else if (isOperationFieldInstruction(operationParam)) {
                StringValue content = getCellContent(nodesToCompile, operationParam);

                return content.getValue();
            } else {
                // TODO FIXME Do not know how to process
                return operationParam;
            }
        } else if (clazz.equals(boolean.class)) {
            return Boolean.parseBoolean(operationParam);
        } else if (clazz.equals(IMethodCaller.class)) {
            if (operationParam == null){
                return null;
            }else{
                IOpenSourceCodeModule src = createSourceCode(nodesToCompile, operationParam);

                OpenL openl = context.getOpenL();

                AlgorithmTreeNode executionNode = getNodeWithResult(nodesToCompile, extractOperationName(operationParam));
                String methodName = operationParam.replace('.', '_') + "_row_" +  executionNode.getAlgorithmRow().getRowNumber();

                IMethodSignature signature = header.getSignature();

                IBindingContext cxt = createBindingContext();

                return OpenlTool.makeMethodWithUnknownType(src, openl, methodName, signature, thisTargetClass, cxt);
            }
        } else {
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation()
                    .asSourceCodeModule();
            throw new BoundError(String.format("Compilation failure. Can't convert parameter %s to type %s",
                    operationParam, clazz.toString()), errorSource);
        }
    }

    /**
     * @param nodesToCompile
     * @return
     * @throws BoundError
     */
    private List<AlgorithmTreeNode> getNestedInstructionsBlock(List<AlgorithmTreeNode> candidateNodes,
            ConversionRuleStep conversionStep) throws BoundError {

        String operationName = extractOperationName(conversionStep.getOperationParam1());
        // We won't extract the field name as it's always the same

        AlgorithmTreeNode executionNode = getNodeWithResult(candidateNodes, operationName);

        return executionNode.getChildren();
    }

    private void declareVariable(List<AlgorithmTreeNode> nodesToCompile, ConversionRuleStep conversionStep)
            throws BoundError {
        String variableNameParameter = conversionStep.getOperationParam1();
        String variableAssignmentParameter = conversionStep.getOperationParam2();

        StringValue variableName = getCellContent(nodesToCompile, variableNameParameter);

        IOpenClass variableType = getTypeOfFieldValue(nodesToCompile, variableAssignmentParameter);

        IOpenField field = new DynamicObjectField(thisTargetClass, variableName.getValue(), variableType);

        thisTargetClass.addField(field);
    }

     /********************************
     * Helper methods For main logic
     ********************************/

    private String generateOpenClassName() {
        return header.getName();
    }

    /**
     * @param nodesToProcess
     * @param firstNodeIndex
     * @return
     */
    private int getLinkedNodesGroupSize(List<AlgorithmTreeNode> nodesToProcess, int firstNodeIndex) {
        int linkedNodesGroupSize = 1; // just one operation by default

        AlgorithmTreeNode currentNodeToProcess = nodesToProcess.get(firstNodeIndex);
        String currentNodeKeyword = currentNodeToProcess.getSpecification().getKeyword();

        String[] operationNamesToGroup = AlgorithmTableParserManager.instance().whatOperationsToGroup(currentNodeKeyword);

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
     * @throws BoundError
     */
    private ConversionRuleBean getConvertionRule(List<AlgorithmTreeNode> nodesToCompile) throws BoundError {
        assert nodesToCompile.size() > 0;

        List<String> groupedOperationNames = new ArrayList<String>(nodesToCompile.size());

        for (AlgorithmTreeNode node : nodesToCompile) {
            groupedOperationNames.add(node.getSpecification().getKeyword());
        }
        
        String operationGroupName = AlgorithmTableParserManager.instance().whatIsOperationsGroupName(groupedOperationNames);

        boolean isMultilineOperation;
        // we assume that all the operations are either all multiline or not
        isMultilineOperation = nodesToCompile.get(0).getSpecification().isMultiline();

        for (ConversionRuleBean conversionRule : conversionRules) {
            if (conversionRule.getOperation().equals(operationGroupName)
                    && (conversionRule.isMultiLine() == isMultilineOperation)) {
                return conversionRule;
            }
        }

        // No conversion rule found.
        String errorMessage = String
                .format(
                        "The operations sequence is wrong: %2$s (all of them are %3$s). Can't find convertion rule for group: %1$s",
                        operationGroupName, groupedOperationNames, isMultilineOperation ? "multiline" : "not multiline");
        IOpenSourceCodeModule errorSource = nodesToCompile.get(0).getAlgorithmRow().getOperation().asSourceCodeModule();
        throw new BoundError(errorMessage, errorSource);
    }

    /****************************************************
     * Helper methods for step processing logic
     ***************************************************/
    private final String FIELD_SEPARATOR = ".";

    /**
     * @param operationToGetFrom
     */
    private String extractOperationName(String operationToGetFrom) {
        // Get the first token before ".", it will be the name of operation
        return operationToGetFrom.split(Pattern.quote(FIELD_SEPARATOR))[0];
    }

    /**
     * @param operationToGetFrom
     */
    private String extractFieldName(String operationToGetFrom) {
        // Get the first token after ".", it will be the field name
        return operationToGetFrom.split(Pattern.quote(FIELD_SEPARATOR))[1];
    }
    
    private boolean isOperationFieldInstruction(String instruction){
        return instruction.split(Pattern.quote(FIELD_SEPARATOR)).length == 2;
    }

    /**
     * @param candidateNodes
     * @param operationToGetFrom
     * @return
     * @throws BoundError
     */
    private AlgorithmTreeNode getNodeWithResult(List<AlgorithmTreeNode> candidateNodes, String operationName)
            throws BoundError {
        AlgorithmTreeNode executionNode = null;

        for (AlgorithmTreeNode node : candidateNodes) {
            if (operationName.equals(node.getAlgorithmRow().getOperation().getValue())) {
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

    private IOpenSourceCodeModule createSourceCode(List<AlgorithmTreeNode> nodesToCompile, String operationParam)
            throws BoundError {
        StringValue openLCodeValue = getCellContent(nodesToCompile, operationParam);

        return openLCodeValue.asSourceCodeModule();
    }
    
    private IOpenClass getTypeOfFieldValue(List<AlgorithmTreeNode> nodesToCompile, String openlStatementInstruction) throws BoundError {
        IMethodCaller assignmentStatement = (IMethodCaller) convertParam(nodesToCompile, IMethodCaller.class,
                openlStatementInstruction);
        return assignmentStatement.getMethod().getType();
    }

    private StringValue getCellContent(List<AlgorithmTreeNode> candidateNodes, String operationParam) throws BoundError {
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

    private IBindingContext thisContext;

    private IBindingContext createBindingContext() {
        if (thisContext == null) {
            thisContext = new ModuleBindingContext(context, thisTargetClass);
        }
        return thisContext;
    }

}
