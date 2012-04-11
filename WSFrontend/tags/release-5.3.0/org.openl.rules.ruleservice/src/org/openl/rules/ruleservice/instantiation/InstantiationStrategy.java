package org.openl.rules.ruleservice.instantiation;

public interface InstantiationStrategy {
    Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException;
}
