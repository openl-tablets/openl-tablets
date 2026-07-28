package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lombok.RequiredArgsConstructor;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.meta.StringValue;
import org.openl.rules.binding.RulesModuleBindingContextHelper;
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
 */
@RequiredArgsConstructor
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
        for (StringValue label : nodesToCompile.getFirst().getLabels()) {
            var methodName = label.getValue();
            var methodHeader = new OpenMethodHeader(methodName,
                    returnType,
                    IMethodSignature.VOID,
                    thisTargetClass);

            var method = new AlgorithmSubroutineMethod(methodHeader);

            thisTargetClass.addMethod(method);

            // to support parameters free call
            var methodAlternative = new NoParamMethodField(methodName, method);
            thisTargetClass.addField(methodAlternative);

            functions.add(new AlgorithmFunctionCompiler(nodesToCompile, methodContext, method, this));
        }
        var internalLablesOfMethod = AlgorithmCompilerTool
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
        var returnValueInstruction = convertionStep.getOperationParam1();

        IOpenClass returnType;
        if (AlgorithmCompilerTool.isOperationFieldInstruction(returnValueInstruction)) {
            returnType = getTypeOfField(
                    AlgorithmCompilerTool.getCellContent(nodesToCompile, returnValueInstruction, bindingContext),
                    bindingContext);
        } else {
            // TODO add support of specification instruction
            returnType = discoverFunctionType(nodesToCompile.getFirst().getChildren(), bindingContext);
        }
        createAlgorithmInternalMethod(nodesToCompile, returnType, new CompileContext(), bindingContext);

    }

    private void declareSubroutine(List<AlgorithmTreeNode> nodesToCompile, IBindingContext bindingContext) {
        var subroutineContext = new CompileContext();
        // add all labels from main
        subroutineContext.registerGroupOfLabels(mainCompileContext.getExistingLables(), bindingContext);

        createAlgorithmInternalMethod(nodesToCompile, JavaOpenClass.VOID, subroutineContext, bindingContext);
    }

    private void declareVariable(List<AlgorithmTreeNode> nodesToCompile,
                                 ConversionRuleStep conversionStep,
                                 IBindingContext bindingContext) {
        var variableNameParameter = conversionStep.getOperationParam1();
        var variableAssignmentParameter = conversionStep.getOperationParam2();
        StringValue variableName = AlgorithmCompilerTool
                .getCellContent(nodesToCompile, variableNameParameter, bindingContext);
        var variableType = getTypeOfField(
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
        var elementNameParameter = conversionStep.getOperationParam1();

        // Points to the location of the iterable array parameter in the Tbasic
        // table
        //
        var iterableArrayParameter = conversionStep.getOperationParam2();

        // Extract the element name
        //
        StringValue elementName = AlgorithmCompilerTool
                .getCellContent(nodesToCompile, elementNameParameter, bindingContext);

        // Extract the type of the iterable array
        //
        var iterableArrayType = getTypeOfField(
                AlgorithmCompilerTool.getCellContent(nodesToCompile, iterableArrayParameter, bindingContext),
                bindingContext);
        if (!iterableArrayType.isArray()) {
            var errorSource = nodesToCompile.getFirst()
                    .getAlgorithmRow()
                    .getAction()
                    .asSourceCodeModule();
            BindHelper
                    .processError("Compilation failure. The cell should be of the array type", errorSource, bindingContext);
        }
        var elementType = iterableArrayType.getComponentClass();
        initNewInternalVariable(elementName.getValue(), elementType);
    }

    private IOpenClass discoverFunctionType(List<AlgorithmTreeNode> children, IBindingContext bindingContext) {
        // find first RETURN operation
        var returnNodes = findFirstReturn(children);

        if (returnNodes == null || returnNodes.isEmpty()) {
            var lastAction = AlgorithmCompilerTool.getLastExecutableOperation(children)
                    .getAlgorithmRow()
                    .getAction();
            return getTypeOfField(lastAction, bindingContext);
        } else {
            // get RETURN.condition part of instruction
            var fieldWithOpenLStatement = "RETURN.condition"; // returnValueInstruction
            return getTypeOfField(
                    AlgorithmCompilerTool.getCellContent(returnNodes, fieldWithOpenLStatement, bindingContext),
                    bindingContext);
        }
    }

    private static List<AlgorithmTreeNode> findFirstReturn(List<AlgorithmTreeNode> nodes) {
        // FIXME delete this method at all
        List<AlgorithmTreeNode> returnNodeSubList = null;
        for (var i = 0; i < nodes.size() && returnNodeSubList == null; i++) {
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
        var currentOperationIndex = 0;
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
        var src = fieldContent.asSourceCodeModule();
        var openl = context.getOpenL();
        var signature = header.getSignature();

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
        var field = new DynamicObjectField(thisTargetClass, variableName, variableType);
        getThisTargetClass().addField(field);
        variablesStack.peek().add(field);
    }

    public IMethodCaller makeMethod(IOpenSourceCodeModule src, String methodName) {
        var openl = context.getOpenL();
        var signature = header.getSignature();
        var cxt = getAlgorithmBindingContext();

        return OpenLManager.makeMethodWithUnknownType(openl, src, methodName, signature, thisTargetClass, cxt);
    }

    public IMethodCaller makeMethodWithCast(IOpenSourceCodeModule src, String methodName, IOpenClass returnType) {
        var openl = context.getOpenL();
        var signature = header.getSignature();
        // create method header for newly created method
        var header = new OpenMethodHeader(methodName, returnType, signature, thisTargetClass);

        var cxt = getAlgorithmBindingContext();
        RulesModuleBindingContextHelper.compileAllTypesInSignature(header.getSignature(), context);
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

        ConversionRuleBean conversionRule = ConversionRulesController
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

            var nodesToCompile = nodesToProcess.subList(i, i + linkedNodesGroupSize);

            precompileLinkedNodesGroup(nodesToCompile, bindingContext);
        }
    }

    private void preprocessConversionStep(List<AlgorithmTreeNode> nodesToCompile,
                                          ConversionRuleStep conversionStep,
                                          IBindingContext bindingContext) {
        assert !nodesToCompile.isEmpty();
        assert conversionStep != null;

        var operationType = conversionStep.getOperationType();
        if (operationType.startsWith("!") && !operationType.equals(OperationType.CHECK_LABEL.toString())) {
            var preprocessor = operationPreprocessors.get(operationType);
            if (preprocessor == null) {
                var errorSource = nodesToCompile.getFirst()
                        .getAlgorithmRow()
                        .getOperation()
                        .asSourceCodeModule();
                BindHelper.processError("Unknown compilation instruction %s".formatted(operationType),
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
            var nodesToProcess = AlgorithmCompilerTool
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
