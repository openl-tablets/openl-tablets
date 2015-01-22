package org.openl.rules.ruleservice.databinding;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 - 2014 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.beans.BeanGenerator;

public class JAXRSArgumentWrapperGenerator {

    Map<String, Class<?>> props = new HashMap<String, Class<?>>();

    public void addProperty(String name, Class<?> type) {
        props.put(name, type);
    }

    public Class<?> generateClass() throws Exception {
        return generateClass(Thread.currentThread().getContextClassLoader());
    }

    public Class<?> generateClass(ClassLoader classLoader) throws Exception {
        BeanGenerator beanGenerator = new BeanGenerator();
        for (String name : props.keySet()) {
            beanGenerator.addProperty(name, props.get(name));
        }
        beanGenerator.setClassLoader(classLoader);
        return (Class<?>) beanGenerator.createClass();
    }

}
