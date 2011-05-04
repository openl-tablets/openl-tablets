package org.openl.util;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.Method;

public class ClassInfo {

    private static AbstractXmlApplicationContext context = new ClassPathXmlApplicationContext("beans-context.xml");

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        getInfo();
    }

    public static void getInfo() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass("com.exigen.hldsa.rate.ws.service.CalculateComputerPremium");
        getClassMethod(clazz);
        clazz = Thread.currentThread().getContextClassLoader().loadClass("com.exigen.hldsa.rate.ws.service.Product");
        getClassMethod(clazz);
        clazz = Thread.currentThread().getContextClassLoader().loadClass("com.exigen.hldsa.rate.ws.service.Policy");
        getClassMethod(clazz);
        clazz = Thread.currentThread().getContextClassLoader().loadClass("com.exigen.hldsa.rate.ws.service.Computer");
        getClassMethod(clazz);
    }

    private static void getClassMethod(Class calculateComputerPremium) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.out.println("\nClass - " + calculateComputerPremium.getSimpleName() + " - Methods\n ------------");
        for(Method m : calculateComputerPremium.getMethods()) {
            if (!contains(m, Object.class.getMethods())) {
                System.out.println(m.getReturnType().getSimpleName() + " " + m.getName());
            }
        }
    }

    private static boolean contains(Method m, Method... methods) {
        for (Method method : methods) {
            if (m.getName().equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

}
