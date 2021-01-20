package org.openl.rules.ruleservice.databinding;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.AbstractTypeCreator;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;

public class OpenLAegisContext extends AegisContext {

    @Override
    public TypeCreator createTypeCreator() {
        AbstractTypeCreator xmlCreator = createRootTypeCreator();

        Java5TypeCreator j5Creator = new Java5TypeCreator(new OpenLAnnotationReader());
        j5Creator.setNextCreator(createDefaultTypeCreator());
        j5Creator.setConfiguration(getTypeCreationOptions());
        xmlCreator.setNextCreator(j5Creator);

        return xmlCreator;
    }
}
