package org.openl.mapper;

public interface Mapper {

    void map(Object source, Object destination);
    <T> T map (Object source, Class<T> destination); 
}
