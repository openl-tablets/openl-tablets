package org.openl.util.generation;

public class NumberInitializationWriter extends CommonInitializationWriter {

    public String getInitialization(Object value) {       
        return String.format("new %s(\"%s\")", JavaClassGeneratorHelper.filterTypeName(value.getClass()), super.getInitialization(value));
    }

}
