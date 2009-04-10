package org.openl.rules.ruleservice.instantiation;

public class SimpleInstantiationStrategy implements InstantiationStrategy {
    public Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }
}
