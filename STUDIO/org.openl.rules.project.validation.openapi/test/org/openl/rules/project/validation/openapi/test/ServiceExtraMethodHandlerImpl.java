package org.openl.rules.project.validation.openapi.test;

import java.io.Serializable;
import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;

@XmlRootElement(namespace = "http://openapi.generated.openl.org", name = "$1ServiceExtraMethodHandlerImpl")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://openapi.generated.openl.org", name = "$1ServiceExtraMethodHandlerImpl", propOrder = {})
public class ServiceExtraMethodHandlerImpl implements ServiceExtraMethodHandler<SpreadsheetResult>, Serializable {
    public ServiceExtraMethodHandlerImpl() {
    }

    public String toString() {
        return "ServiceExtraMethodHandlerImpl{" + " }";
    }

    public boolean equals(Object var1) {
        if (this == var1) {
            return true;
        } else if (var1 == null) {
            return false;
        } else if (this.getClass() != var1.getClass()) {
            return false;
        } else {
            ServiceExtraMethodHandlerImpl var2 = (ServiceExtraMethodHandlerImpl) var1;
            return true;
        }
    }

    public int hashCode() {
        return 5;
    }

    public SpreadsheetResult invoke(Method var1, Object var2, Object[] var3) {
        return null;
    }
}
