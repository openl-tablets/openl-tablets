package org.openl.mapper.demo;

public class XmlBeanFactory {
    public static Object newInstance(Class<?> clazz) {
         return new org.dozer.factory.XMLBeanFactory().createBean(null, clazz, clazz.getName());
    }
}
