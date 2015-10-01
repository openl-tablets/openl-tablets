package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.*;

@XmlRootElement(name="table")
@XmlType(name = "table")
public class TableImpl implements Table {
    private String name;
    private List<ParameterImpl> parameters = new ArrayList<ParameterImpl>();
    private String returnType;
    private List<ConditionImpl> horizontalConditions = new ArrayList<ConditionImpl>();
    private List<ConditionImpl> verticalConditions = new ArrayList<ConditionImpl>();
    private List<ReturnRow> returnValues = new ArrayList<ReturnRow>();
    private SegmentImpl segment;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @XmlElement(defaultValue = "String")
    @Override
    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @XmlElementWrapper(name="horizontalConditions", required = true)
    @XmlElement(name = "condition")
    @Override
    public List<ConditionImpl> getHorizontalConditions() {
        return horizontalConditions;
    }

    public void setHorizontalConditions(List<ConditionImpl> horizontalConditions) {
        this.horizontalConditions = horizontalConditions;
    }

    @XmlElementWrapper(name="verticalConditions", required = true)
    @XmlElement(name = "condition")
    @Override
    public List<ConditionImpl> getVerticalConditions() {
        return verticalConditions;
    }

    public void setVerticalConditions(List<ConditionImpl> verticalConditions) {
        this.verticalConditions = verticalConditions;
    }

    @XmlElementWrapper(name="returnValues", required = true)
    @XmlElement(name = "list")
    @Override
    public List<ReturnRow> getReturnValues() {
        return returnValues;
    }

    public void setReturnValues(List<ReturnRow> returnValues) {
        this.returnValues = returnValues;
    }

    @Override
    public SegmentImpl getSegment() {
        return segment;
    }

    public void setSegment(SegmentImpl segment) {
        this.segment = segment;
    }
}
