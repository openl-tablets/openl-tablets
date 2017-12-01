package org.openl.rules.ruleservice.publish.jaxws;

import javax.xml.ws.WebFault;

@WebFault(name="Exception", faultBean="org.openl.rules.ruleservice.publish.jaxws.FaultInfo", targetNamespace="http://jaxws.publish.ruleservice.rules.openl.org")
public class JAXWSException extends RuntimeException{
    
    private static final long serialVersionUID = 6412876579527950740L;

    private FaultInfo faultInfo;
    
    public JAXWSException(String message, FaultInfo faultInfo){
        super(message);
        this.faultInfo = faultInfo;
    }
    
    public JAXWSException(String message, FaultInfo faultInfo, Throwable cause){
        super(message, cause);
        this.faultInfo = faultInfo;
    }
    
    public FaultInfo getFaultInfo() {
        return faultInfo;
    }
}
