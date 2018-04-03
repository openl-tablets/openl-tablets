package org.openl.rules.ruleservice.publish.jaxws;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.helpers.DOMUtils;
import org.openl.rules.ruleservice.core.ExceptionType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JAXWSException extends SoapFault {

    private static final long serialVersionUID = 6412876579527950740L;

    private static String buildMessage(ExceptionType type, String message) {
        return "[" + type.toString() + "] " + message;
    }

    public JAXWSException(ExceptionType type, String message) {
        super(buildMessage(type, message), SoapFault.FAULT_CODE_SERVER);
    }

    public void setDetail(String stacktrace) {
        Document document = DOMUtils.createDocument();
        Element detail = document.createElement("detail");
        detail.setTextContent(stacktrace);
        super.setDetail(detail);
    }

}
