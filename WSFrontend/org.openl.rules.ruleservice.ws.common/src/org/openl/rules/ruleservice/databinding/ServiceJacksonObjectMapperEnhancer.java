package org.openl.rules.ruleservice.databinding;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.databinding.jackson.NonNullMixIn;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ServiceJacksonObjectMapperEnhancer {
    private static final AtomicLong incrementer = new AtomicLong();

    private final ObjectMapper objectMapper;
    private final XlsModuleOpenClass xlsModuleOpenClass;
    private final ClassLoader classLoader;

    public ServiceJacksonObjectMapperEnhancer(ObjectMapper objectMapper,
            XlsModuleOpenClass xlsModuleOpenClass,
            ClassLoader classLoader) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.xlsModuleOpenClass = Objects.requireNonNull(xlsModuleOpenClass, "xlsModuleOpenClass cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader cannot be null");
    }

    private Class<?> enhanceMixInClass(Class<?> originalMixInClass, Class<?> sprBeanClass, ClassLoader classLoader) {
        String className = originalMixInClass.getName() + "$Enhanced$" + incrementer.getAndIncrement();
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new SpreadsheetResultBeanClassMixInAnnotationsWriter(classWriter,
                className,
                originalMixInClass,
                sprBeanClass);
            InterfaceTransformer transformer = new InterfaceTransformer(originalMixInClass, className, true);
            transformer.accept(classVisitor);
            classWriter.visitEnd();
            try {
                ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);
                return Class.forName(className, true, classLoader);
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public ObjectMapper createObjectMapper() {
        for (IOpenClass openClass : xlsModuleOpenClass.getTypes()) {
            if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                Class<?> sprBeanClass = ((CustomSpreadsheetResultOpenClass) openClass).getBeanClass();
                addMixInAnnotationsToSprBeanClass(objectMapper, sprBeanClass);
            }
        }

        Class<?> sprBeanClass = xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
            .toCustomSpreadsheetResultOpenClass()
            .getBeanClass();
        addMixInAnnotationsToSprBeanClass(objectMapper, sprBeanClass);
        return objectMapper;
    }

    private void addMixInAnnotationsToSprBeanClass(ObjectMapper objectMapper, Class<?> sprBeanClass) {
        Class<?> originalMixInClass = objectMapper.findMixInClassFor(sprBeanClass);
        Class<?> mixInClass;
        if (originalMixInClass == null) {
            mixInClass = NonNullMixIn.class;
        } else {
            mixInClass = enhanceMixInClass(originalMixInClass, sprBeanClass, classLoader);
        }
        objectMapper.addMixIn(sprBeanClass, mixInClass);
    }
}
