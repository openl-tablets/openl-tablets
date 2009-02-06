package org.openl.rules.tbasic.compile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmRow;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TableParserManager;
import org.openl.rules.tbasic.runtime.RuntimeOperation;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

public class AlgorithmCompiler {
    /********************************
     * Initial data
     *******************************/
    private IBindingContext context;
    private IOpenMethodHeader header;
    private List<AlgorithmTreeNode> parsedNodes;

    /********************************
     * Intermediate values
     *******************************/
    private ConversionRuleBean[] conversionRules;
    private LabelManager labelManager;

    /*********************************
     * Compiler output
     ********************************/
    private List<RuntimeOperation> operations;
    private ModuleOpenClass thisTargetClass;
    private Map<String, RuntimeOperation> labelsRegister;

    /*********************************
     * Properties
     ********************************/

    /**
     * @return the operations
     */
    public List<RuntimeOperation> getOperations() {
        return operations;
    }

    /**
     * @param operations
     *            the operations to set
     */
    public void setOperations(List<RuntimeOperation> operations) {
        this.operations = operations;
    }

    /**
     * @return the thisTarget
     */
    public IOpenClass getThisTargetClass() {
        return thisTargetClass;
    }




    /*********************************
     * Constructors
     ********************************/


    public AlgorithmCompiler(IBindingContext context, IOpenMethodHeader header, List<AlgorithmTreeNode> parsedNodes){
        this.context = context;
        this.header = header;
        this.parsedNodes = parsedNodes;
    }

    /*********************************
     * Methods
     ********************************/

    public void compile(Algorithm algorithm) {
        operations = new ArrayList<RuntimeOperation>();
        thisTargetClass = new ModuleOpenClass(null, generateOpenClassName(), context.getOpenL()); 
        labelsRegister = new HashMap<String, RuntimeOperation>();
        conversionRules = fixBrokenValues(TableParserManager.instance().getConversionRules());
        labelManager = new LabelManager();

        preProcess();
        process();

        algorithm.setThisClass(getThisTargetClass());
        algorithm.setAlgorithmSteps(getOperations());
        algorithm.setLabels(getLabels());
    }

    private ConversionRuleBean[] fixBrokenValues(ConversionRuleBean[] conversionRules) {
        for (ConversionRuleBean conversionRule : conversionRules){
            fixBrokenValues(conversionRule.getOperationType());
            fixBrokenValues(conversionRule.getOperationParam1());
            fixBrokenValues(conversionRule.getOperationParam2());
            fixBrokenValues(conversionRule.getLabel());
        }
        return conversionRules;
    }

    private void fixBrokenValues(String[] label) {
        for (int i = 0; i < label.length; i++){
            if (label[i].toUpperCase().equals("N/A")){
                label[i] = null;
            }
        }
        
    }

    private void process() {
        operations.addAll(process(parsedNodes));
    }

    private List<RuntimeOperation> process(List<AlgorithmTreeNode> nodes) {
        List<RuntimeOperation> emittedOperations = new ArrayList<RuntimeOperation>();

        int shiftToNextToGroupOperation = 1;
        
        for (int i = 0; i < nodes.size(); i += shiftToNextToGroupOperation) {
            // get nodes to generate code from

            AlgorithmTreeNode parsedNode = nodes.get(i);

            String[] operationNamesToGroup = TableParserManager.instance().whatOperationsToGroup(
                    parsedNode.getSpecification().getKeyword());
            
            shiftToNextToGroupOperation = 1;
            
            if (operationNamesToGroup != null){
                List<String> operationsToGroupWithCurrent = Arrays.asList(operationNamesToGroup);

                for (; shiftToNextToGroupOperation < nodes.size() - i; shiftToNextToGroupOperation++) {
                    AlgorithmTreeNode groupCandidateNode = nodes.get(i + shiftToNextToGroupOperation);
                    if (!operationsToGroupWithCurrent.contains(groupCandidateNode.getSpecification().getKeyword())) {
                        break;
                    }
                }
            }

            List<AlgorithmTreeNode> nodesToCompile = nodes.subList(i, i + shiftToNextToGroupOperation);

            emittedOperations.addAll(compileLinkedNodes(nodesToCompile));
        }

        return emittedOperations;
    }

    List<RuntimeOperation> compileLinkedNodes(List<AlgorithmTreeNode> nodesToCompile) {
        List<RuntimeOperation> emitedOperations = new ArrayList<RuntimeOperation>();

        // FIXME
        ConversionRuleBean conversionRule = getConvertionRule(nodesToCompile);

        labelManager.startOperationsSet(getOperationsType(nodesToCompile));

        labelManager.generateAllLabels(conversionRule.getLabel());
        
        boolean isAppliedLabel = false;
        String userDefinedLabel = getUserDefinedLabel(nodesToCompile);

        for (int i = 0; i < conversionRule.getOperationType().length; i++) {
            String operationType = conversionRule.getOperationType()[i];
            String operationParam1 = conversionRule.getOperationParam1()[i];
            String operationParam2 = conversionRule.getOperationParam2()[i];
            String labelInstruction = conversionRule.getLabel()[i];

            String label = null;
            RuntimeOperation emmitedOperation = null;

            if (labelInstruction != null) {
                label = labelManager.getLabelByInstruction(labelInstruction);
            }

            // TODO
            if (!operationType.startsWith("!")) {
                emmitedOperation = createOperation(nodesToCompile, operationType, operationParam1, operationParam2);
                emitedOperations.add(emmitedOperation);
            } else if (operationType.equals("!Compile")) {
                List<AlgorithmTreeNode> nodesToProcess;
                nodesToProcess = getNestedInstructionsBlock(nodesToCompile, operationParam1);
                emitedOperations.addAll(process(nodesToProcess));
            } else {
                // TODO perform other operations
            }

            if (emmitedOperation != null && label != null) {
                labelsRegister.put(label, emmitedOperation);
            }
            
            // TODO: complex tricky implemented logic:
            // apply user defined label to the first emitted operation
            if (userDefinedLabel != null && emmitedOperation != null && !isAppliedLabel){
                labelsRegister.put(userDefinedLabel, emmitedOperation);
                isAppliedLabel = true;
            }

        }

        labelManager.finishOperationsSet();

        return emitedOperations;
    }

    private String getUserDefinedLabel(List<AlgorithmTreeNode> nodesToCompile) {
        AlgorithmTreeNode allowedToBeLabeledNode = nodesToCompile.get(0);
        
        String labelValue = allowedToBeLabeledNode.getAlgorithmRow().getLabel().getValue();

        return labelValue;
    }

    private boolean getOperationsType(List<AlgorithmTreeNode> nodesToCompile) {
        // FIXME add the field to operation definition
        final List<String> loopOperations = Arrays.asList(new String[] { "WHILE", "FOR EACH" });
        String operationsKeyword = nodesToCompile.get(0).getSpecification().getKeyword();
        return loopOperations.contains(operationsKeyword);
    }

    /**
     * @param nodesToCompile
     * @return
     */
    private List<AlgorithmTreeNode> getNestedInstructionsBlock(List<AlgorithmTreeNode> candidateNodes,
            String operationToGetFrom) {

        String operationName = extractOperationName(operationToGetFrom);
        // We won't extract the field name as it's always the same

        AlgorithmTreeNode executionNode = getNodeWithResult(candidateNodes, operationName);

        return executionNode.getChildren();
    }

    /**
     * @param operationToGetFrom
     */
    private String extractOperationName(String operationToGetFrom) {
        // TODO
        // Get the first token before ".", it will be the name of operation
        return operationToGetFrom.split("\\.")[0];
    }

    /**
     * @param operationToGetFrom
     */
    private String extractFieldName(String operationToGetFrom) {
        // TODO
        // Get the first token after ".", it will be the field name
        return operationToGetFrom.split("\\.")[1];
    }

    /**
     * @param candidateNodes
     * @param operationToGetFrom
     * @return
     */
    private AlgorithmTreeNode getNodeWithResult(List<AlgorithmTreeNode> candidateNodes, String operationName) {
        AlgorithmTreeNode executionNode = null;

        for (AlgorithmTreeNode node : candidateNodes) {
            if (operationName.equals(node.getAlgorithmRow().getOperation().getValue())) {
                executionNode = node;
            }
        }

        if (executionNode == null) {
            throw new RuntimeException("Compilation strange. Couldn't find......");
        }
        return executionNode;
    }

    private RuntimeOperation createOperation(List<AlgorithmTreeNode> nodesToCompile, String operationType,
            String operationParam1, String operationParam2) {
        try {
            Class clazz = Class.forName("org.openl.rules.tbasic.runtime." + operationType + "Operation");
            Constructor constructor = clazz.getConstructors()[0];

            Object[] params = new Object[constructor.getParameterTypes().length];

            if (constructor.getParameterTypes().length > 0) {
                params[0] = convertParam(nodesToCompile, constructor.getParameterTypes()[0], operationParam1);
            }

            if (constructor.getParameterTypes().length > 1) {
                params[1] = convertParam(nodesToCompile, constructor.getParameterTypes()[1], operationParam2);
            }

            // FIXME put source reference

            return (RuntimeOperation) constructor.newInstance(params);

        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private Object convertParam(List<AlgorithmTreeNode> nodesToCompile, Class clazz, String operationParam) {
        // FIXME !!!!

        if (clazz.equals(String.class)) {
            if (labelManager.isLabelInstruction(operationParam)) {
                return labelManager.getLabelByInstruction(operationParam);
            } else {
                StringValue content = getCellContent(nodesToCompile, operationParam);
                
                return content.getValue();
            }
        } else if (clazz.equals(boolean.class)) {
            return Boolean.parseBoolean(operationParam);
        } else if (clazz.equals(IMethodCaller.class)){
            
            IOpenSourceCodeModule src = createSourceCode(nodesToCompile, operationParam);
            
            OpenL openl = context.getOpenL();
            
            AlgorithmTreeNode executionNode = getNodeWithResult(nodesToCompile, extractOperationName(operationParam));
            String methodName = operationParam.replace('.', '_') + executionNode.getAlgorithmRow().getRowNumber();
            
            IMethodSignature signature = header.getSignature();
            
            IBindingContext cxt = createBindingContext();
            
            return OpenlTool.makeMethodWithUnknownType(src, openl, methodName, signature, thisTargetClass, cxt);
        } else {
            // FIXME
            throw new RuntimeException("Unknown type");
        }
    }

    private IOpenSourceCodeModule createSourceCode(List<AlgorithmTreeNode> nodesToCompile, String operationParam) {
        StringValue openLCodeValue = getCellContent(nodesToCompile, operationParam);

        return openLCodeValue.asSourceCodeModule();
    }

    private StringValue getCellContent(List<AlgorithmTreeNode> candidateNodes, String operationParam) {
        String operationName = extractOperationName(operationParam);
        String fieldName = extractFieldName(operationParam);

        AlgorithmTreeNode executionNode = getNodeWithResult(candidateNodes, operationName);

        IOpenField codeField = JavaOpenClass.getOpenClass(AlgorithmRow.class).getField(fieldName);

        if (codeField == null) {
            // TODO
            throw new RuntimeException("Instruction wrong....");
        }

        StringValue openLCode = (StringValue) codeField.get(executionNode.getAlgorithmRow(), null);

        return openLCode;
    }

    private IBindingContext thisContext;
    private IBindingContext createBindingContext() {
        if (thisContext == null){
            thisContext = new ModuleBindingContext(context, thisTargetClass);
        }
        return thisContext;
    }

    private String generateOpenClassName() {
        return header.getName();
    }

    private ConversionRuleBean getConvertionRule(List<AlgorithmTreeNode> nodesToCompile) {
        // TODO rewrite to be rule
        String operationName;

        // add first operation name
        operationName = nodesToCompile.get(0).getSpecification().getKeyword();
        // add the second if it is ELSE

        if (nodesToCompile.size() > 1) {
            String secondKeyword = nodesToCompile.get(1).getSpecification().getKeyword();
            if (secondKeyword.equals("ELSE")) {
                operationName += secondKeyword;
            }
        }
        boolean isMultilineOperation;
        // we assume that all the operations are either all multiline or not
        isMultilineOperation = nodesToCompile.get(0).getSpecification().isMultiLine();

        for (ConversionRuleBean conversionRule : conversionRules) {
            if (conversionRule.getOperation().equals(operationName)
                    && (conversionRule.isMultiLine() == isMultilineOperation)) {
                return conversionRule;
            }
        }

        // TODO
        throw new RuntimeException("Smth wrong. didn't find convertion rule");
    }

    /**
     * 
     */
    private void preProcess() {

    }

    public Map<String, RuntimeOperation> getLabels() {
        return labelsRegister;
    }
}
