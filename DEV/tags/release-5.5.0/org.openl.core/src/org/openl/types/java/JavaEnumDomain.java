package org.openl.types.java;

import java.util.Iterator;

import org.openl.domain.FixedSizeDomain;
import org.openl.domain.IType;
import org.openl.util.OpenIterator;

public class JavaEnumDomain extends FixedSizeDomain<Object> {
    
    private JavaOpenEnum enumClass;
    
    public JavaEnumDomain(JavaOpenEnum enumClass) {
        this.enumClass = enumClass;
    }

    @SuppressWarnings("unchecked")
    public Iterator iterator() {
        return OpenIterator.fromArray(enumClass.getInstanceClass().getEnumConstants());
    }

    public int size() {
        return enumClass.getInstanceClass().getEnumConstants().length;
    }

    public IType getElementType() {
        return enumClass;
    }

    public boolean selectObject(Object obj) {
        return enumClass.getInstanceClass().isInstance(obj);
    }

    public boolean selectType(IType type) {
        return type == enumClass;
    }

    public Object getValue(int index) {
        return enumClass.getInstanceClass().getEnumConstants()[index];
    }

}
