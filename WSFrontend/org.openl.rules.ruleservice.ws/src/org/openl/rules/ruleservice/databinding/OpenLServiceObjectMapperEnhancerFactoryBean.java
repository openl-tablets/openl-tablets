package org.openl.rules.ruleservice.databinding;

import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.databinding.jackson.NonNullMixIn;
import org.openl.rules.serialization.DatatypeOpenClassMixInAnnotationsWriter;
import org.openl.rules.serialization.jackson.NonEmptyMixIn;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class OpenLServiceObjectMapperEnhancerFactoryBean extends AbstractFactoryBean<ObjectMapper> {
    private static final AtomicLong incrementer = new AtomicLong();

    private JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean;

    public JacksonObjectMapperFactoryBean getJacksonObjectMapperFactoryBean() {
        return jacksonObjectMapperFactoryBean;
    }

    public void setJacksonObjectMapperFactoryBean(JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean) {
        this.jacksonObjectMapperFactoryBean = jacksonObjectMapperFactoryBean;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    protected ObjectMapper createInstance() throws Exception {
        ObjectMapper objectMapper = getJacksonObjectMapperFactoryBean().createJacksonObjectMapper();
        OpenLService openLService = OpenLServiceHolder.getInstance().get();
        if (openLService == null) {
            throw new ServiceConfigurationException("Failed to locate a service.");
        }
        if (openLService.getOpenClass() != null) {
            for (IOpenClass openClass : openLService.getOpenClass().getTypes()) {
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    Class<?> sprBeanClass = ((CustomSpreadsheetResultOpenClass) openClass).getBeanClass();
                    addMixInAnnotationsToSprBeanClass(objectMapper, sprBeanClass, openLService.getClassLoader());
                }
            }
            if (openLService.getOpenClass() instanceof XlsModuleOpenClass) {
                XlsModuleOpenClass xlsModuleOpenClass = ((XlsModuleOpenClass) openLService.getOpenClass());
                if (xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                    Class<?> sprBeanClass = xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                        .toCustomSpreadsheetResultOpenClass()
                        .getBeanClass();
                    addMixInAnnotationsToSprBeanClass(objectMapper, sprBeanClass, openLService.getClassLoader());
                }
            }
            for (IOpenClass type : openLService.getOpenClass().getTypes()) {
                if (type instanceof DatatypeOpenClass) {
                    addMixInAnnotationsToDatatype(objectMapper, type.getInstanceClass(), openLService.getClassLoader());
                }
            }
        }
        return objectMapper;
    }

    private void addMixInAnnotationsToDatatype(ObjectMapper objectMapper,
            Class<?> datatypeClass,
            ClassLoader classLoader) {
        Class<?> originalMixInClass = objectMapper.findMixInClassFor(datatypeClass);
        Class<?> mixInClass = enhanceMixInClassForDatatypeClass(
            originalMixInClass != null ? originalMixInClass : NonEmptyMixIn.class,
            classLoader);
        objectMapper.addMixIn(datatypeClass, mixInClass);
    }

    private void addMixInAnnotationsToSprBeanClass(ObjectMapper objectMapper,
            Class<?> sprBeanClass,
            ClassLoader classLoader) {
        Class<?> originalMixInClass = objectMapper.findMixInClassFor(sprBeanClass);
        Class<?> mixInClass;
        if (originalMixInClass == null) {
            mixInClass = NonNullMixIn.class;
        } else {
            mixInClass = enhanceMixInClassForSprBeanClass(originalMixInClass, classLoader);
        }
        objectMapper.addMixIn(sprBeanClass, mixInClass);
    }

    private Class<?> enhanceMixInClassForSprBeanClass(Class<?> originalMixInClass, ClassLoader classLoader) {
        String className = originalMixInClass.getName() + "$Enhanced$" + incrementer.getAndIncrement();
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new SpreadsheetResultBeanClassMixInAnnotationsWriter(classWriter,
            className,
            originalMixInClass);
        return defineAndLoadClass(originalMixInClass, classLoader, className, classWriter, classVisitor);
    }

    private Class<?> enhanceMixInClassForDatatypeClass(Class<?> originalMixInClass, ClassLoader classLoader) {
        String className = originalMixInClass.getName() + "$Enhanced$" + incrementer.getAndIncrement();
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new DatatypeOpenClassMixInAnnotationsWriter(classWriter,
            className,
            originalMixInClass);

        return defineAndLoadClass(originalMixInClass, classLoader, className, classWriter, classVisitor);
    }

    private Class<?> defineAndLoadClass(Class<?> originalMixInClass,
            ClassLoader classLoader,
            String className,
            ClassWriter classWriter,
            ClassVisitor classVisitor) {
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
