package org.openl.rules.tbasic.compile;

import java.lang.reflect.Constructor;
import java.util.List;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

/**
 * Factory for creating TBasic operations from the 'org.openl.rules.tbasic.runtime.operations' package
 *
 * Created by dl on 9/16/14.
 */
public class OperationFactory {

    private static final String OPERATIONS_PACKAGE = "org.openl.rules.tbasic.runtime.operations";
    private static final String OPERATION_SUFFIX = "Operation";

    private ParameterConverterManager parameterConverter;

    public OperationFactory(ParameterConverterManager parameterConverter) {
        this.parameterConverter = parameterConverter;
    }

    public RuntimeOperation createOperation(List<AlgorithmTreeNode> nodesToCompile,
            ConversionRuleStep conversionStep) throws Exception {
        try {
            Constructor<?> constructor = getOperationConstructor(conversionStep.getOperationType());

            Object[] params = new Object[constructor.getParameterTypes().length];

            // Init the first parameter for the Operation constructor
            //
            if (constructor.getParameterTypes().length > 0) {
                params[0] = parameterConverter.convertParam(nodesToCompile,
                    constructor.getParameterTypes()[0],
                    conversionStep.getOperationParam1());
            }

            // Init the second parameter for the Operation constructor
            //
            if (constructor.getParameterTypes().length > 1) {
                params[1] = parameterConverter.convertParam(nodesToCompile,
                    constructor.getParameterTypes()[1],
                    conversionStep.getOperationParam2());
            }

            RuntimeOperation emittedOperation = (RuntimeOperation) constructor.newInstance(params);

            // TODO: set more precise source reference
            AlgorithmOperationSource source = AlgorithmCompilerTool.getOperationSource(nodesToCompile,
                conversionStep.getOperationParam1());
            emittedOperation.setSourceCode(source);

            String nameForDebug = conversionStep.getNameForDebug();
            emittedOperation.setNameForDebug(nameForDebug);

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
        String operationClassName = String.format("%s.%s%s", OPERATIONS_PACKAGE, operationType, OPERATION_SUFFIX);
        Class<?> clazz = Class.forName(operationClassName);
        return clazz.getConstructors()[0];
    }
}
