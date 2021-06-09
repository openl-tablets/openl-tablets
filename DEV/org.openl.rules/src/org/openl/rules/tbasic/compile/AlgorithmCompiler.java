package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.NoParamMethodField;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;

// FIXME: !!!!!!!!!!!!!!! refactor to eliminate code duplications and to isolate
// different functionality in separate classes

/**
 * @author User
 *
 */
public class AlgorithmCompiler {
    /***************************************************************************
     * Initial data
     **************************************************************************/
    private final IBindingContext context;
    private final IOpenMethodHeader header;
    private final List<AlgorithmTreeNode> nodesToCompile;

    /***************************************************************************
     * Intermediate values
     **************************************************************************/
    private CompileContext mainCompileContext;
    private final List<AlgorithmFunctionCompiler> functions = new ArrayList<>();
    private LabelManager labelManager;

    /***************************************************************************
     * Compiler output
     **************************************************************************/
    private AlgorithmOpenClass thisTargetClass;

    private IBindingContext thisContext;

    private final Map<String, OperationPreprocessor> operationPreprocessors = new HashMap<>();

    private final Stack<Collection<IOpenField>> variablesStack = new Stack<>();

    {
        operationPreprocessors.put(OperationType.COMPILE.toString(), new CompilePreprocessor());
        operationPreprocessors.put(OperationType.DECLARE.toString(), new DeclarePreprocessor());
        operationPreprocessors.put(OperationType.DECLARE_ARRAY_ELEMENT.toString(),
            new DeclareArrayElementPreprocessor());
        operationPreprocessors.put(OperationType.SUBROUTINE.toString(), new DeclareSubroutinePreprocessor());
        operationPreprocessors.put(OperationType.FUNCTION.toString(), new DeclareFunctionPreprocessor());
    }

    public AlgorithmCompiler(IBindingContext context,
            IOpenMethodHeader header,
            List<AlgorithmTreeNode> nodesToCompile) {
        this.context = context;
        this.header = header;
        this.nodesToCompile = nodesToCompile;
    }

    /***************************************************************************
     * Main logic
     **************************************************************************/

    public void compile(Algorithm algorithm, IBindingContext bindingContext) {
        initialization(algorithm, bindingContext);
        precompileNestedNodes(nodesToCompile, bindingContext);
        compile(bindingContext);
        postprocess(algorithm);
    }

    private void compile(IBindingContext bindingContext) {
        getThisTargetClass().allFieldsToVisible();
        for (AlgorithmFunctionCompiler functionCompiler : functions) {
            functionCompiler.compile(bindingContext);
        }
    }

    private void createAlgorithmInternalMethod(List<AlgorithmTreeNode> nodesToCompile,
            IOpenClass returnType,
            CompileContext methodContext,
            IBindingContext bindingContext) {
        // method name will be at every label
        for (StringValue label : nodesToCompile.get(0).getLabels()) {
            String methodName = label.getValue();
            IOpenMethodHeader methodHeader = new OpenMethodHeader(methodName,
                returnType,
                IMethodSignature.VOID,
                thisTargetClass);

            AlgorithmSubroutineMethod method = new AlgorithmSubroutineMethod(methodHeader);

            thisTargetClass.addMethod(method);

            // to support parameters free call
            NoParamMethodField methodAlternative = new NoParamMethodField(methodName, method);
            thisTargetClass.addField(methodAlternative);

            functions.add(new AlgorithmFunctionCompiler(nodesToCompile, methodContext, method, this));
        }
        Map<String, AlgorithmTreeNode> internalLablesOfMethod = AlgorithmCompilerTool
            .getAllDeclaredLables(nodesToCompile);
        methodContext.registerGroupOfLabels(internalLablesOfMethod, bindingContext);
    }

    private IBindingContext getAlgorithmBindingContext() {
        if (thisContext == null) {
            thisContext = new ComponentBindingContext(context, thisTargetClass);
        }
        return thisContext;
    }

    private void declareFunction(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep convertionStep,
            IBindingContext bindingContext) {
        String returnValueInstruction = convertionStep.getOperationParam1();

        IOpenClass returnType;
        if (AlgorithmCompilerTool.isOperationFieldInstruction(returnValueInstruction)) {
            returnType = getTypeOfField(
                AlgorithmCompilerTool.getCellContent(nodesToCompile, returnValueInstruction, bindingContext),
                bindingContext);
        } else {
            // TODO add support of specification instruction
            returnType = discoverFunctionType(nodesToCompile.get(0).getChildren(), bindingContext);
        }
        createAlgorithmInternalMethod(nodesToCompile, returnType, new CompileContext(), bindingContext);

    }

    private void declareSubroutine(List<AlgorithmTreeNode> nodesToCompile, IBindingContext bindingContext) {
        CompileContext subroutineContext = new CompileContext();
        // add all labels from main
        subroutineContext.registerGroupOfLabels(mainCompileContext.getExistingLables(), bindingContext);

        createAlgorithmInternalMethod(nodesToCompile, JavaOpenClass.VOID, subroutineContext, bindingContext);
    }

    private void declareVariable(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep,
            IBindingContext bindingContext) {
        String variableNameParameter = conversionStep.getOperationParam1();
        String variableAssignmentParameter = conversionStep.getOperationParam2();
        StringValue variableName = AlgorithmCompilerTool
            .getCellContent(nodesToCompile, variableNameParameter, bindingContext);
        IOpenClass variableType = getTypeOfField(
            AlgorithmCompilerTool.getCellContent(nodesToCompile, variableAssignmentParameter, bindingContext),
            bindingContext);
        initNewInternalVariable(variableName.getValue(), variableType);
    }

    /**
     * Find out the type of the array element. And define the internal variable
     */
    private void declareArrayElement(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep,
            IBindingContext bindingContext) {
        // Points to the location of the elementName in the TBasic table
        //
        String elementNameParameter = conversionStep.getOperationParam1();

        // Points to the location of the iterable array parameter in the Tbasic
        // table
        //
        String iterableArrayParameter = conversionStep.getOperationParam2();

        // Extract the element name
        //
        StringValue elementName = AlgorithmCompilerTool
            .getCellContent(nodesToCompile, elementNameParameter, bindingContext);

        // Extract the type of the iterable array
        //
        IOpenClass iterableArrayType = getTypeOfField(
            AlgorithmCompilerTool.getCellContent(nodesToCompile, iterableArrayParameter, bindingContext),
            bindingContext);
        if (!iterableArrayType.isArray()) {
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0)
                .getAlgorithmRow()
                .getAction()
                .asSourceCodeModule();
            BindHelper
                .processError("Compilation failure. The cell should be of the array type", errorSource, bindingContext);
        }
        IOpenClass elementType = iterableArrayType.getComponentClass();
        initNewInternalVariable(elementName.getValue(), elementType);
    }

    private IOpenClass discoverFunctionType(List<AlgorithmTreeNode> children, IBindingContext bindingContext) {
        // find first RETURN operation
        List<AlgorithmTreeNode> returnNodes = findFirstReturn(children);

        if (returnNodes == null || returnNodes.isEmpty()) {
            StringValue lastAction = AlgorithmCompilerTool.getLastExecutableOperation(children)
                .getAlgorithmRow()
                .getAction();
            return getTypeOfField(lastAction, bindingContext);
        } else {
            // get RETURN.condition part of instruction
            String fieldWithOpenLStatement = "RETURN.condition"; // returnValueInstruction
            return getTypeOfField(
                AlgorithmCompilerTool.getCellContent(returnNodes, fieldWithOpenLStatement, bindingContext),
                bindingContext);
        }
    }

    private static List<AlgorithmTreeNode> findFirstReturn(List<AlgorithmTreeNode> nodes) {
        // FIXME delete this method at all
        List<AlgorithmTreeNode> returnNodeSubList = null;
        for (int i = 0; i < nodes.size() && returnNodeSubList == null; i++) {
            if (TBasicSpecificationKey.RETURN.toString().equals(nodes.get(i).getSpecificationKeyword())) {
                returnNodeSubList = nodes.subList(i, i + 1);
            } else if (nodes.get(i).getChildren() != null) {
                returnNodeSubList = findFirstReturn(nodes.get(i).getChildren());
            }
        }
        return returnNodeSubList;
    }

    private String generateOpenClassName() {
        return header.getName();
    }

    public LabelManager getLabelManager() {
        return labelManager;
    }

    /***************************************************************************
     * Helper methods
     **************************************************************************/

    private List<AlgorithmTreeNode> getMainFunctionBody() {
        int currentOperationIndex = 0;
        while (currentOperationIndex < nodesToCompile.size() && !TBasicSpecificationKey.FUNCTION.toString()
            .equals(nodesToCompile.get(currentOperationIndex).getSpecificationKeyword()) && !TBasicSpecificationKey.SUB
                .toString()
                .equals(nodesToCompile.get(currentOperationIndex).getSpecificationKeyword())) {
            currentOperationIndex++;
        }
        return nodesToCompile.subList(0, currentOperationIndex);
    }

    public AlgorithmOpenClass getThisTargetClass() {
        return thisTargetClass;
    }

    public IOpenClass getTypeOfField(StringValue fieldContent, IBindingContext bindingContext) {
        // TODO: make rational type detecting(without creating of
        // CompositeMethod)
        IOpenSourceCodeModule src = fieldContent.asSourceCodeModule();
        OpenL openl = context.getOpenL();
        IMethodSignature signature = header.getSignature();

        return OpenLManager
            .makeMethodWithUnknownType(openl,
                src,
                "cell_" + fieldContent.getValue(),
                signature,
                thisTargetClass,
                getAlgorithmBindingContext())
            .getMethod()
            .getType();
    }

    private void initialization(Algorithm algorithm, IBindingContext bindingContext) {
        labelManager = new LabelManager();
        thisTargetClass = new AlgorithmOpenClass(generateOpenClassName(), context.getOpenL());

        variablesStack.push(new ArrayList<>());
        initNewInternalVariable("ERROR", JavaOpenClass.getOpenClass(RuntimeException.class));
        initNewInternalVariable("Error Message", JavaOpenClass.STRING);

        mainCompileContext = new CompileContext();
        List<AlgorithmTreeNode> mainFunction = getMainFunctionBody();
        mainCompileContext.registerGroupOfLabels(AlgorithmCompilerTool.getAllDeclaredLables(mainFunction),
            bindingContext);
        functions.add(new AlgorithmFunctionCompiler(mainFunction, mainCompileContext, algorithm, this));
    }

    private void initNewInternalVariable(String variableName, IOpenClass variableType) {
        IOpenField field = new DynamicObjectField(thisTargetClass, variableName, variableType);
        getThisTargetClass().addField(field);
        variablesStack.peek().add(field);
    }

    public IMethodCaller makeMethod(IOpenSourceCodeModule src, String methodName) {
        OpenL openl = context.getOpenL();
        IMethodSignature signature = header.getSignature();
        IBindingContext cxt = getAlgorithmBindingContext();

        return OpenLManager.makeMethodWithUnknownType(openl, src, methodName, signature, thisTargetClass, cxt);
    }

    public IMethodCaller makeMethodWithCast(IOpenSourceCodeModule src, String methodName, IOpenClass returnType) {
        OpenL openl = context.getOpenL();
        IMethodSignature signature = header.getSignature();
        // create method header for newly created method
        OpenMethodHeader header = new OpenMethodHeader(methodName, returnType, signature, thisTargetClass);

        IBindingContext cxt = getAlgorithmBindingContext();
        return OpenLManager.makeMethod(openl, src, header, cxt);

    }

    private void postprocess(Algorithm algorithm) {
        for (AlgorithmFunctionCompiler functionCompiler : functions) {
            functionCompiler.postprocess();
        }
        algorithm.setThisClass(getThisTargetClass());
    }

    private void precompileLinkedNodesGroup(List<AlgorithmTreeNode> nodesToCompile, IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();

        ConversionRuleBean conversionRule = ConversionRulesController.getInstance()
            .getConvertionRule(nodesToCompile, bindingContext);
        if (conversionRule == null) {
            return;
        }

        for (ConversionRuleStep convertionStep : conversionRule.getConvertionSteps()) {
            preprocessConversionStep(nodesToCompile, convertionStep, bindingContext);
        }
    }

    private void precompileNestedNodes(List<AlgorithmTreeNode> nodesToProcess, IBindingContext bindingContext) {
        // process nodes by groups of linked nodes
        for (int i = 0, linkedNodesGroupSize; i < nodesToProcess.size(); i += linkedNodesGroupSize) {
            linkedNodesGroupSize = AlgorithmCompilerTool.getLinkedNodesGroupSize(nodesToProcess, i);

            List<AlgorithmTreeNode> nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);

            precompileLinkedNodesGroup(nodesToCompile, bindingContext);
        }
    }

    private void preprocessConversionStep(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep,
            IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();
        assert conversionStep != null;

        String operationType = conversionStep.getOperationType();
        if (operationType.startsWith("!") && !operationType.equals(OperationType.CHECK_LABEL.toString())) {
            OperationPreprocessor preprocessor = operationPreprocessors.get(operationType);
            if (preprocessor == null) {
                IOpenSourceCodeModule errorSource = nodesToCompile.get(0)
                    .getAlgorithmRow()
                    .getOperation()
                    .asSourceCodeModule();
                BindHelper.processError(String.format("Unknown compilation instruction %s", operationType),
                    errorSource,
                    bindingContext);
            } else {
                preprocessor.preprocess(nodesToCompile, conversionStep, bindingContext);
            }
        }
    }

    public interface OperationPreprocessor {
        void preprocess(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext);
    }

    private final class CompilePreprocessor implements OperationPreprocessor {

        @Override
        public void preprocess(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            List<AlgorithmTreeNode> nodesToProcess = AlgorithmCompilerTool
                .getNestedInstructionsBlock(nodesToCompile, conversionStep.getOperationParam1(), bindingContext);
            try {
                variablesStack.push(new ArrayList<>());
                precompileNestedNodes(nodesToProcess, bindingContext);
            } finally {
                updateVariablesVisibitily(variablesStack.pop());
            }
        }
    }

    private void updateVariablesVisibitily(Collection<IOpenField> fields) {
        for (IOpenField field : fields) {
            thisTargetClass.setFieldToInvisibleState(field.getName());
        }
    }

    private final class DeclarePreprocessor implements OperationPreprocessor {

        @Override
        public void preprocess(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            declareVariable(nodesToCompile, conversionStep, bindingContext);
        }
    }

    private final class DeclareArrayElementPreprocessor implements OperationPreprocessor {

        @Override
        public void preprocess(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            declareArrayElement(nodesToCompile, conversionStep, bindingContext);
        }
    }

    private final class DeclareSubroutinePreprocessor implements OperationPreprocessor {

        @Override
        public void preprocess(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            declareSubroutine(nodesToCompile, bindingContext);
        }
    }

    private final class DeclareFunctionPreprocessor implements OperationPreprocessor {

        @Override
        public void preprocess(List<AlgorithmTreeNode> nodesToCompile,
                ConversionRuleStep conversionStep,
                IBindingContext bindingContext) {
            declareFunction(nodesToCompile, conversionStep, bindingContext);
        }
    }

}
