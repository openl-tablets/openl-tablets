package org.openl.rules.ruleservice.publish.jaxws;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.openl.rules.ruleservice.publish.common.ExceptionResponseDto;
import org.w3c.dom.Element;

import javassist.util.proxy.MethodHandler;

public class JAXWSMethodHandler implements MethodHandler {

    private final Object service;
    private final Map<Method, Method> methodMap;

    public JAXWSMethodHandler(Object service, Map<Method, Method> methodMap) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
        this.methodMap = Objects.requireNonNull(methodMap, "methodMap cannot be null");
    }

    @Override
    public Object invoke(Object o, Method method, Method proceed, Object[] args) {
        Method m = methodMap.get(method);
        try {
            return m.invoke(service, args);
        } catch (Exception e) {

            ExceptionResponseDto dto = ExceptionResponseDto.createFrom(e);

            // Create a standard fault
            SoapFault fault = new SoapFault(dto.getMessage(), Fault.FAULT_CODE_SERVER);

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
