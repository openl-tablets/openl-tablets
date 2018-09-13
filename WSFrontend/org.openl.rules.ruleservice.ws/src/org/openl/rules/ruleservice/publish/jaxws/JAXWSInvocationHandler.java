package org.openl.rules.ruleservice.publish.jaxws;

import org.apache.cxf.binding.soap.SoapFault;
import org.openl.rules.ruleservice.publish.common.ExceptionResponseDto;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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

            ExceptionResponseDto dto = ExceptionResponseDto.createFrom(e);

            // Create a standart fault
            SoapFault fault = new SoapFault(dto.getMessage(), SoapFault.FAULT_CODE_SERVER);

            // <detail> <type>TYPE</type> <stackTrace>stacktrace of cause</stackTrace> </detail>
            Element detailEl = fault.getOrCreateDetail();
            Element typeEl = detailEl.getOwnerDocument().createElement("type");
            typeEl.setTextContent(dto.getType());
            detailEl.appendChild(typeEl);

            if (dto.getDetail() != null) {
                Element stackTraceEl = detailEl.getOwnerDocument().createElement("stackTrace");
                stackTraceEl.setTextContent(dto.getDetail());
                detailEl.appendChild(stackTraceEl);
            }

            throw fault;
        }
    }
}
