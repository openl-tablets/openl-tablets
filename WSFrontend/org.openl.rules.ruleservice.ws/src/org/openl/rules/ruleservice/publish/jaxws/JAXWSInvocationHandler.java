package org.openl.rules.ruleservice.publish.jaxws;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.binding.soap.SoapFault;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.w3c.dom.Element;

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

            String message = t.getMessage();
            ExceptionType type = ExceptionType.SYSTEM;
            if (t instanceof RuleServiceWrapperException) {
                RuleServiceWrapperException ruleServiceWrapperException = (RuleServiceWrapperException) t;
                type = ruleServiceWrapperException.getType();
                message = ruleServiceWrapperException.getSimpleMessage();
            }

            // Create a standart fault
            SoapFault fault = new SoapFault(message, SoapFault.FAULT_CODE_SERVER);

            // <detail> <type>TYPE</type> <stackTrace>stacktrace of cause</stackTrace> </detail>
            Element detailEl = fault.getOrCreateDetail();
            Element typeEl = detailEl.getOwnerDocument().createElement("type");
            typeEl.setTextContent(type.toString());
            detailEl.appendChild(typeEl);

            if (!ExceptionType.USER_ERROR.equals(type)) {
                Element stackTraceEl = detailEl.getOwnerDocument().createElement("stackTrace");
                stackTraceEl.setTextContent(ExceptionUtils.getStackTrace(e.getCause()));
                detailEl.appendChild(stackTraceEl);
            }

            throw fault;
        }
    }
}
