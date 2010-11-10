package org.openl.mapper.mapping;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.dozer.CustomConverter;
import org.openl.mapper.Mapping;

public class MappingProcessor {

    private Class<?> instanceClass;
    private Object instance;

    public MappingProcessor(Class<?> instanceClass, Object instance) {
        this.instanceClass = instanceClass;
        this.instance = instance;
    }

    public Collection<Bean2BeanMappingDescriptor> loadMappings() {
        List<Mapping> mappings = findMappings(instanceClass, instance);

        return processMappings(mappings);
    }

    private List<Mapping> findMappings(Class<?> interfaceClass, Object instance) {

        List<Mapping> mappings = new ArrayList<Mapping>();
        Collection<Method> declarations = findMappingDeclarations(interfaceClass);

        for (Method declaration : declarations) {
            Mapping[] fieldMappings;

            try {
                fieldMappings = (Mapping[]) declaration.invoke(instance, new Object[0]);
            } catch (Exception e) {
                throw new RuntimeException("Cannot load mappings", e);
            }

            mappings.addAll(Arrays.asList(fieldMappings));
        }

        return mappings;
    }

    private Collection<Method> findMappingDeclarations(Class<?> instanceClass) {

        Method[] methods = instanceClass.getMethods();

        Predicate predicate = new Predicate() {
            public boolean evaluate(Object arg0) {
                Method method = (Method) arg0;
                return method.getReturnType().isArray() && method.getReturnType().getComponentType() == Mapping.class;
            }
        };

        Collection<Method> mappingDeclarations = new ArrayList<Method>();
        CollectionUtils.select(Arrays.asList(methods), predicate, mappingDeclarations);

        return mappingDeclarations;
    }

    private Collection<Bean2BeanMappingDescriptor> processMappings(Collection<Mapping> mappings) {

        Map<ClassPair, Bean2BeanMappingDescriptor> beanMappings = new HashMap<ClassPair, Bean2BeanMappingDescriptor>();

        for (Mapping mapping : mappings) {
            Class<?> classA = mapping.getClassA();
            Class<?> classB = mapping.getClassB();

            Bean2BeanMappingDescriptor beanMapping = findBeanMapping(beanMappings, classA, classB);
            Bean2BeanMappingDescriptor reverseBeanMapping = findBeanMapping(beanMappings, classB, classA);

            if (beanMapping == null) {
                beanMapping = new Bean2BeanMappingDescriptor();
                beanMapping.setClassA(classA);
                beanMapping.setClassB(classB);
                beanMappings.put(new ClassPair(classA, classB), beanMapping);

                reverseBeanMapping = new Bean2BeanMappingDescriptor();
                reverseBeanMapping.setClassA(classB);
                reverseBeanMapping.setClassB(classA);

                beanMappings.put(new ClassPair(classB, classA), reverseBeanMapping);
            }

            beanMapping.getFieldMappings().add(createFieldMapping(mapping));

            if (!mapping.isOneWay()) {
                reverseBeanMapping.getFieldMappings().add(createFieldMapping(reverseFieldMapping(mapping)));
            }
        }

        return beanMappings.values();
    }

    private Field2FieldMappingDescriptor createFieldMapping(Mapping mapping) {

        Field2FieldMappingDescriptor fieldMapping = new Field2FieldMappingDescriptor();
        fieldMapping.setFieldA(mapping.getFieldA());
        fieldMapping.setFieldB(mapping.getFieldB());

        if (!StringUtils.isBlank(mapping.getConvertMethod())) {
            String converterId = createConverterId(mapping);
            ConverterDescriptor converterDescriptor = createConverterDescriptor(converterId, mapping.getConvertMethod());
            fieldMapping.setConverter(converterDescriptor);
        }

        return fieldMapping;
    }

    private ConverterDescriptor createConverterDescriptor(String converterId, String convertMethodName) {
        CustomConverter converterInstance = createConverterProxy(convertMethodName);
        return new ConverterDescriptor(converterId, converterInstance);
    }

    private String createConverterId(Mapping mapping) {
        if (!StringUtils.isBlank(mapping.getConvertMethod())) {
            return StringUtils.join(new String[] { mapping.getClassA().getName(), mapping.getFieldA(), "_",
                    mapping.getClassB().getName(), mapping.getFieldB() }, ".");
        }

        return null;
    }

    private CustomConverter createConverterProxy(final String convertMethodName) {

        if (!StringUtils.isEmpty(convertMethodName)) {
            Class<?>[] interfaces = new Class[] { CustomConverter.class };
            ClassLoader classLoader = MappingProcessor.class.getClassLoader();

            return (CustomConverter) Proxy.newProxyInstance(classLoader, interfaces, new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                    // Parameters list of CustomConverter.convert method :
                    // 1) Object existingDestinationFieldValue,
                    // 2) Object sourceFieldValue,
                    // 3) Class<?> destinationClass,
                    // 4) Class<?> sourceClass);
                    //
                    Class<?> destClass = (Class<?>) args[2];
                    Class<?> srcClass = (Class<?>) args[3];
                    Class<?>[] parameterTypes = new Class<?>[] { srcClass, destClass };

                    Method convertMethod = instanceClass.getMethod(convertMethodName, parameterTypes);

                    Object destValue = args[0];
                    Object srcValue = args[1];
                    Object[] parameterValues = new Object[] { srcValue, destValue };

                    Object result = convertMethod.invoke(instance, parameterValues);

                    return result;
                }
            });
        }

        return null;
    }

    private Mapping reverseFieldMapping(Mapping mapping) {

        if (mapping == null || mapping.isOneWay()) {
            return null;
        }

        Mapping reverseMapping = new Mapping();
        reverseMapping.setClassA(mapping.getClassB());
        reverseMapping.setClassB(mapping.getClassA());
        reverseMapping.setFieldA(mapping.getFieldB());
        reverseMapping.setFieldB(mapping.getFieldA());
        reverseMapping.setConvertMethod(mapping.getConvertMethod());
        reverseMapping.setOneWay(mapping.isOneWay());

        return reverseMapping;
    }

    private Bean2BeanMappingDescriptor findBeanMapping(Map<ClassPair, Bean2BeanMappingDescriptor> beanMappings,
        Class<?> classA, Class<?> classB) {

        ClassPair pair = new ClassPair(classA, classB);

        if (beanMappings.containsKey(pair)) {
            return beanMappings.get(pair);
        }

        return null;
    }

}
