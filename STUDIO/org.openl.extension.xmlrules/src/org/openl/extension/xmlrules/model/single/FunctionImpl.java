package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.FunctionExpression;
import org.openl.extension.xmlrules.model.Parameter;

@XmlRootElement(name="function")
@XmlType(name = "function")
public class FunctionImpl implements Function {
    private String name;
    private String returnType;
    private List<ParameterImpl> parameters = new ArrayList<ParameterImpl>();
    private String cellAddress;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    @XmlElementWrapper(name="parameters", required = true)
    @XmlElement(name = "parameter")
    @Override
    public List<ParameterImpl> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterImpl> parameters) {
        this.parameters = parameters;
    }

    @XmlElement(name = "cellAddress")
    @Override
    public String getCellAddress() {
        return cellAddress;
    }

    public void setCellAddress(String cellAddress) {
        this.cellAddress = cellAddress;
    }

}
