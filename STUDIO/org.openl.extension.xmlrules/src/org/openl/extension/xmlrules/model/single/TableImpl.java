package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import org.openl.extension.xmlrules.model.*;

public class TableImpl implements Table {
    private String name;
    private List<Parameter> parameters = new ArrayList<Parameter>();
    private String returnType;
    private List<Condition> horizontalConditions = new ArrayList<Condition>();
    private List<Condition> verticalConditions = new ArrayList<Condition>();
    private List<List<ReturnValue>> returnValues = new ArrayList<List<ReturnValue>>();
    private SegmentImpl segment;
    private XlsRegionImpl region;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public List<Condition> getHorizontalConditions() {
        return horizontalConditions;
    }

    public void setHorizontalConditions(List<Condition> horizontalConditions) {
        this.horizontalConditions = horizontalConditions;
    }

    @Override
    public List<Condition> getVerticalConditions() {
        return verticalConditions;
    }

    public void setVerticalConditions(List<Condition> verticalConditions) {
        this.verticalConditions = verticalConditions;
    }

    @Override
    public XlsRegionImpl getRegion() {
        return region;
    }

    public void setRegion(XlsRegionImpl region) {
        this.region = region;
    }

    @Override
    public List<List<ReturnValue>> getReturnValues() {
        return returnValues;
    }

    public void setReturnValues(List<List<ReturnValue>> returnValues) {
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
