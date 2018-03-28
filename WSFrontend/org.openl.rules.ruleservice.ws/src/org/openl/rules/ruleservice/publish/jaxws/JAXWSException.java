package org.openl.rules.ruleservice.publish.jaxws;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.helpers.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JAXWSException extends SoapFault {

    private static final long serialVersionUID = 6412876579527950740L;

    public JAXWSException(String message) {
        super(message, SoapFault.FAULT_CODE_SERVER);
    }

    public void setDetail(String type, String stacktrace) {
        Document document = DOMUtils.createDocument();
        Element detail = document.createElement("detail");
        detail.setTextContent("Error: " + type + System.lineSeparator() + stacktrace);
        super.setDetail(detail);
    }

}
