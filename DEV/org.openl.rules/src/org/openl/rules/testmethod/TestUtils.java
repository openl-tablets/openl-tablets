package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.exception.OpenLException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.syntax.exception.CompositeSyntaxNodeException;

public final class TestUtils {

    public static ParameterWithValueDeclaration[] getContextParams(TestSuite test, TestDescription testCase) {
        List<ParameterWithValueDeclaration> params = new ArrayList<ParameterWithValueDeclaration>();

        TestSuiteMethod testMethod = test.getTestSuiteMethod();
        IRulesRuntimeContext context = testCase.getRuntimeContext();

        for (int i = 0; i < testMethod.getColumnsCount(); i++) {
            String columnName = testMethod.getColumnName(i);
            if (columnName != null && columnName.startsWith(TestMethodHelper.CONTEXT_NAME)) {

                Object value = context != null ? context.getValue(columnName.replace(TestMethodHelper.CONTEXT_NAME + ".", "")) : null;

                params.add(new ParameterWithValueDeclaration(columnName, value));
            }
        }

        return params.toArray(new ParameterWithValueDeclaration[params.size()]);
    }

    public static List<OpenLMessage> getUserMessagesAndErrors(Object error) {
        if (error instanceof Throwable) {
            Throwable exception = (Throwable) error;
            exception = ExceptionUtils.getRootCause(exception);
            List<OpenLMessage> messages = new ArrayList<OpenLMessage>();

            if (exception instanceof OpenLUserRuntimeException) {
                OpenLUserRuntimeException userException = (OpenLUserRuntimeException) exception;
                messages.add(new OpenLMessage(userException.getOriginalMessage()));
            } else if (exception instanceof CompositeSyntaxNodeException) {
                CompositeSyntaxNodeException compositeException = (CompositeSyntaxNodeException) exception;

                for (OpenLException openLException : compositeException.getErrors()) {
                    if (openLException instanceof OpenLUserRuntimeException) {
                        OpenLUserRuntimeException userException = (OpenLUserRuntimeException) openLException;
                        messages.add(new OpenLMessage(userException.getOriginalMessage()));
                    } else {
                        messages.add(new OpenLErrorMessage(openLException));
                    }
                }

            } else {
                if (exception instanceof OpenLException) {
                    messages.add(new OpenLErrorMessage((OpenLException) exception));
                } else {
                    messages.add(new OpenLErrorMessage(ExceptionUtils.getRootCauseMessage(exception)));
                }
            }

            return messages;
        }

        return Collections.emptyList();
    }

    public static List<OpenLMessage> getErrors(Object error) {
        if (error instanceof Throwable) {
            return OpenLMessagesUtils.newMessages((Throwable) error);
        }

        return Collections.emptyList();
    }

}
