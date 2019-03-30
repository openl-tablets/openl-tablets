package org.openl.rules.tbasic.runtime;

import java.lang.reflect.InvocationTargetException;

import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * The <code>AlgorithmErrorHelper</code> class is the utility class which
 * works with errors(handling and creation).
 *
 */
class AlgorithmErrorHelper {
    
    private AlgorithmErrorHelper() {
    }

    /**
     * Create exception with specified message and makes reference to source of
     * error.
     *
     * @param message The message to display.
     * @param operation The operation which is source of error.
     * @return <code>OpenLAlgorithmExecutionException</code> with wanted error
     *         message.
     */
    public static OpenLAlgorithmExecutionException createExecutionException(String message, RuntimeOperation operation) {
        String sourceOperationUrl = operation.getSourceCode().getSourceUri();
        String errorMessage = String
                .format(
                        "Unexpected error appeared while executing TBasic component logic. It's unusal situation and the most propably something is wrong in component's internal logic, please contact developers. Error: %s at %s",
                        message, sourceOperationUrl);

        return new OpenLAlgorithmExecutionException(errorMessage);
    }

    /**
     * Try to process specified error occurred within environment.<br>
     * Function "ON ERROR" must be specified to handle error by user.
     *
     * @param error
     * @param environment
     * @return Result of the execution "ON ERROR" method(if specified).
     */
    public static Object processError(Throwable error, TBasicContextHolderEnv environment) {
        IOpenClass algorithmType = environment.getTbasicTarget().getType();
        IOpenMethod errorMethod = algorithmType.getMethod("ON ERROR", new IOpenClass[] {});

        if (errorMethod != null) {
            IOpenField errorField = algorithmType.getField("ERROR");
            if (errorField != null) {
                // extracting ERROR exception
                Throwable err = ((InvocationTargetException) error.getCause().getCause()).getTargetException();
                errorField.set(environment.getTbasicTarget(), err, environment);
                IOpenField errorMessageField = algorithmType.getField("Error Message");
                errorMessageField.set(environment.getTbasicTarget(), err.getMessage(), environment);
            }
            return errorMethod.invoke(environment.getTbasicTarget(), null, environment);
        }

        // throw new RuntimeException(String.format("Execution of algorithm
        // failed: %s", error.getMessage()), error);
        throw RuntimeExceptionWrapper.wrap(error);

    }

}
