package org.openl.rules.ruleservice.helper;

public class SimpleInstantiationStrategy implements InstantiationStrategy {
    public Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }
}
