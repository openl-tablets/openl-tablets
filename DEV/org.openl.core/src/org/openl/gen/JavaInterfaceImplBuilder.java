package org.openl.gen;

import java.beans.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author Vladyslav Pikus
 */
public class JavaInterfaceImplBuilder {

    private static final String NAME_PATTERN = "org.openl.generated.$%o%sImpl";
    private static final AtomicInteger counter = new AtomicInteger();

    private final Class<?> clazzInterface;
    private final String beanName;
    private final Map<String, FieldDescription> beanFields = new TreeMap<>();
    private final List<MethodDescription> beanStubMethods = new ArrayList<>();

    public JavaInterfaceImplBuilder(Class<?> clazzInterface) {
        if (!clazzInterface.isInterface()) {
            throw new IllegalArgumentException("Target class is not an interface.");
        }
        this.clazzInterface = clazzInterface;
        this.beanName = String.format(NAME_PATTERN, counter.incrementAndGet(), clazzInterface.getSimpleName());
        init();
    }

    private void init() {
        Set<Method> usedMethods = new HashSet<>();
        try {
            for (Class<?> it : clazzInterface.getInterfaces()) {
                collectFieldsAndMethods(it, usedMethods);
            }
            collectFieldsAndMethods(clazzInterface, usedMethods);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private void collectFieldsAndMethods(Class<?> clazzInterface,
            Set<Method> usedMethods) throws IntrospectionException {

        BeanInfo info = Introspector.getBeanInfo(clazzInterface);
        PropertyDescriptor[] properties = info.getPropertyDescriptors();
        for (PropertyDescriptor property : properties) {
            usedMethods.add(property.getReadMethod());
            usedMethods.add(property.getWriteMethod());
            beanFields.put(property.getName(), new FieldDescription(property.getPropertyType().getName()));
        }
        MethodDescriptor[] methods = info.getMethodDescriptors();
        for (MethodDescriptor method : methods) {
            if (!usedMethods.contains(method.getMethod())) {
                Method methodRef = method.getMethod();
                MethodDescription methodDescription = new MethodDescription(method.getName(),
                    methodRef.getReturnType(),
                    methodRef.getParameterTypes());

                if (!beanStubMethods.contains(methodDescription)) {
                    beanStubMethods.add(methodDescription);
                }
            }
        }
    }

    public String getBeanName() {
        return beanName;
    }

    public byte[] byteCode() {
        return new JavaInterfaceImplGenerator(beanName, clazzInterface, beanFields, beanStubMethods).byteCode();
    }

}
