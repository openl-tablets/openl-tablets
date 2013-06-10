package org.openl.ruleservice.dynamicinterface.test;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

public class MyBeforeAdvice implements ServiceMethodBeforeAdvice {

    public void before(Method method, Object proxy, Object... args) throws Throwable {
        MyClass myClass = (MyClass) args[1];
        myClass.setName("beforeAdviceWasInvoked");
    }
}
