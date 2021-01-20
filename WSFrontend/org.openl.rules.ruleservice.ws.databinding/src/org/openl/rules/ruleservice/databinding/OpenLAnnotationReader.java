package org.openl.rules.ruleservice.databinding;

import java.lang.reflect.AnnotatedElement;

import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.java5.AnnotationReader;

public class OpenLAnnotationReader extends AnnotationReader {

    @Override
    public Class<?> getType(AnnotatedElement element) {
        Class<?> type = super.getType(element);
        if (type == null || !AegisType.class.isAssignableFrom(type)) {
            return null;
        }
        return type;
    }

}
