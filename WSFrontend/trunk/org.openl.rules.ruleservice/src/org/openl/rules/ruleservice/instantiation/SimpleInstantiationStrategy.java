package org.openl.rules.ruleservice.instantiation;

public class SimpleInstantiationStrategy extends AClassInstantiationStrategy {

    public SimpleInstantiationStrategy(Class<?> clazz) {
        super(clazz);
    }

    public Object instantiate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        return clazz.newInstance();
    }
}
