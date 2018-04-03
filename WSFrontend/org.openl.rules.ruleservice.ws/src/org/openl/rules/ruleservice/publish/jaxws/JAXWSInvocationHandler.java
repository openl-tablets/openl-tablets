package org.openl.rules.ruleservice.publish.jaxws;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;

public class JAXWSInvocationHandler implements InvocationHandler {

    private Object target;

    public JAXWSInvocationHandler(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("target argument must not be null!");
        }
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {

            Throwable t = e;
            while (t instanceof InvocationTargetException || t instanceof UndeclaredThrowableException) {
                if (t instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) t).getTargetException();
                }
                if (t instanceof UndeclaredThrowableException) {
                    t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
                }
            }

            if (t instanceof RuleServiceWrapperException) {
                RuleServiceWrapperException ruleServiceWrapperException = (RuleServiceWrapperException) t;
                boolean detailedFault = ExceptionType.SYSTEM
                    .equals(ruleServiceWrapperException.getType()) || ExceptionType.RULES_RUNTIME
                        .equals(ruleServiceWrapperException.getType()) || ExceptionType.COMPILATION
                            .equals(ruleServiceWrapperException.getType());
                JAXWSException jaxwsException = new JAXWSException(ruleServiceWrapperException.getType(), ruleServiceWrapperException.getSimpleMessage());
                if (detailedFault) {
                    jaxwsException.setDetail(ExceptionUtils.getStackTrace(e.getCause()));
                }
                throw jaxwsException;
            } else {
                JAXWSException jaxwsException = new JAXWSException(ExceptionType.SYSTEM, t.getMessage());
                jaxwsException.setDetail(ExceptionUtils.getStackTrace(e.getCause()));
                throw jaxwsException;
            }
        }
    }
}