package org.openl.rules.lang.xls.utils;

public interface ICustomCaster<TypeToCastFrom, TypeToCastTo> {
    
    public TypeToCastTo cast(TypeToCastFrom value);
    
}
