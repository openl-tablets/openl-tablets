package org.openl.rules.tbasic.compile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * Converts the parameter defined in the TBasic table specification to the appropriate Operation constructor parameter
 *
 * Created by dl on 9/16/14.
 */
public class ParameterConverterManager {
    private final AlgorithmCompiler compiler;

    /** return type for some contexts that are represented as functions **/
    private final IOpenClass returnType;

    private final LabelManager labelManager;

    private final Map<Class<?>, ParameterConverter> parameterConverters = new HashMap<>();

    {
        parameterConverters.put(String.class, new StringConverter());
        parameterConverters.put(boolean.class, new BooleanConverter());
        parameterConverters.put(IMethodCaller.class, new MethodCallerConverter());
    }

    public ParameterConverterManager(AlgorithmCompiler compiler, IOpenClass returnType) {
        this.compiler = compiler;
        this.returnType = returnType;
        this.labelManager = compiler.getLabelManager();
    }

    public Object convertParam(List<AlgorithmTreeNode> nodesToCompile,
            Class<?> clazz,
            String operationParam,
            IBindingContext bindingContext) {

        ParameterConverter converter = parameterConverters.get(clazz);

        if (converter == null) {
            IOpenSourceCodeModule errorSource = nodesToCompile.get(0)
                .getAlgorithmRow()
                .getOperation()
                .asSourceCodeModule();
            BindHelper.processError(String.format("Compilation failure. Cannot convert parameter %s to type %s",
                operationParam,
                clazz.toString()), errorSource, bindingContext);
        }

        return converter.convert(nodesToCompile, operationParam, bindingContext);
    }

    public interface ParameterConverter {
        Object convert(List<AlgorithmTreeNode> nodesToCompile, String operationParam, IBindingContext bindingContext);
    }

    private final class StringConverter implements ParameterConverter {

        @Override
        public Object convert(List<AlgorithmTreeNode> nodesToCompile,
                String operationParam,
                IBindingContext bindingContext) {
            if (labelManager.isLabelInstruction(operationParam)) {
                return labelManager.getLabelByInstruction(operationParam);
            } else if (AlgorithmCompilerTool.isOperationFieldInstruction(operationParam)) {
                StringValue content = AlgorithmCompilerTool
                    .getCellContent(nodesToCompile, operationParam, bindingContext);

                return content.getValue();
            } else {
                // TODO FIXME Do not know how to process
                return operationParam;
            }
        }
    }

    private static final class BooleanConverter implements ParameterConverter {

        @Override
        public Object convert(List<AlgorithmTreeNode> nodesToCompile,
                String operationParam,
                IBindingContext bindingContext) {
            return Boolean.parseBoolean(operationParam);
        }
    }

    private final class MethodCallerConverter implements ParameterConverter {

        @Override
        public Object convert(List<AlgorithmTreeNode> nodesToCompile,
                String operationParam,
                IBindingContext bindingContext) {
            if (operationParam == null) {
                return null;
            }

            StringValue cellContent = AlgorithmCompilerTool
                .getCellContent(nodesToCompile, operationParam, bindingContext);

            AlgorithmTreeNode executionNode = AlgorithmCompilerTool
                .extractOperationNode(nodesToCompile, operationParam, bindingContext);
            String methodName = String
                .format("%s_row_%s", operationParam.replace('.', '_'), executionNode.getAlgorithmRow().getRowNumber());

            IOpenSourceCodeModule src = cellContent.getMetaInfo().getSource();
            // return statements for the whole Algorithm(TBasic) should be casted to the return type of
            // whole Algorithm rule
            if (labelManager.isReturnInstruction(operationParam)) {
                /** create method and cast its value to the appropriate return type */
                return compiler.makeMethodWithCast(src, methodName, returnType);
            } else {
                return compiler.makeMethod(src, methodName);
            }
        }
    }
}
