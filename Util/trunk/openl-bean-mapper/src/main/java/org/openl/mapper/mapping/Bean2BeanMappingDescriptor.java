package org.openl.mapper.mapping;

import java.util.ArrayList;
import java.util.List;

public class Bean2BeanMappingDescriptor {

    private Class<?> classA;
    private Class<?> classB;
    private boolean classAXmlBean;
    private boolean classBXmlBean;

    private List<Field2FieldMappingDescriptor> fieldMappings = new ArrayList<Field2FieldMappingDescriptor>();

    public Class<?> getClassA() {
        return classA;
    }

    public void setClassA(Class<?> classA) {
        this.classA = classA;
    }

    public Class<?> getClassB() {
        return classB;
    }

    public void setClassB(Class<?> classB) {
        this.classB = classB;
    }
    
    public boolean isClassAXmlBean() {
        return classAXmlBean;
    }

    public void setClassAXmlBean(boolean classAXmlBean) {
        this.classAXmlBean = classAXmlBean;
    }

    public boolean isClassBXmlBean() {
        return classBXmlBean;
    }

    public void setClassBXmlBean(boolean classBXmlBean) {
        this.classBXmlBean = classBXmlBean;
    }

    public List<Field2FieldMappingDescriptor> getFieldMappings() {
        return fieldMappings;
    }

}
