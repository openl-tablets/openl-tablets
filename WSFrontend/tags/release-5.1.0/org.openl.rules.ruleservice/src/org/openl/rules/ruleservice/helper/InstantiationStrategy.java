package org.openl.rules.ruleservice.helper;

public interface InstantiationStrategy {
    Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException;
}
