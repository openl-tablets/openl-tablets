package org.openl.rules.ruleservice.databinding;

import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.databinding.jackson.NonNullMixIn;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JacksonObjectMapperEnhancerFactoryBean extends AbstractFactoryBean<ObjectMapper> {
    private static final AtomicLong incrementer = new AtomicLong();

    private ProjectJacksonObjectMapperFactoryBean projectJacksonObjectMapperFactoryBean;

    public ProjectJacksonObjectMapperFactoryBean getProjectJacksonObjectMapperFactoryBean() {
        return projectJacksonObjectMapperFactoryBean;
    }

    public void setProjectJacksonObjectMapperFactoryBean(
            ProjectJacksonObjectMapperFactoryBean projectJacksonObjectMapperFactoryBean) {
        this.projectJacksonObjectMapperFactoryBean = projectJacksonObjectMapperFactoryBean;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    private Class<?> enhanceMixInClass(Class<?> originalMixInClass, ClassLoader classLoader) {
        if (originalMixInClass.isInterface()) {
            String className = originalMixInClass.getName() + "$Enhanced$" + incrementer.getAndIncrement();
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                ClassWriter classWriter = new ClassWriter(0);
                ClassVisitor classVisitor = new SpreadsheetResultBeanClassMixInAnnotationsWriter(classWriter,
                    className,
                    originalMixInClass);
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
        return originalMixInClass;
    }

    @Override
    protected ObjectMapper createInstance() throws Exception {
        ObjectMapper objectMapper = getProjectJacksonObjectMapperFactoryBean().createJacksonObjectMapper();
        OpenLService openLService = OpenLServiceHolder.getInstance().get();
        if (openLService == null) {
            throw new ServiceConfigurationException("Failed to locate a service.");
        }
        if (openLService.getOpenClass() != null) {
            for (IOpenClass openClass : openLService.getOpenClass().getTypes()) {
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    Class<?> sprBeanClass = ((CustomSpreadsheetResultOpenClass) openClass).getBeanClass();
                    addMixInAnnotationsToSprBeanClass(objectMapper, openLService, sprBeanClass);
                }
            }
            if (openLService.getOpenClass() instanceof XlsModuleOpenClass) {
                Class<?> sprBeanClass = ((XlsModuleOpenClass) openLService.getOpenClass())
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass()
                    .getBeanClass();
                addMixInAnnotationsToSprBeanClass(objectMapper, openLService, sprBeanClass);
            }
        }
        return objectMapper;
    }

    private void addMixInAnnotationsToSprBeanClass(ObjectMapper objectMapper,
            OpenLService openLService,
            Class<?> sprBeanClass) throws RuleServiceInstantiationException {
        Class<?> originalMixInClass = objectMapper.findMixInClassFor(sprBeanClass);
        Class<?> mixInClass;
        if (originalMixInClass == null) {
            mixInClass = NonNullMixIn.class;
        } else {
            mixInClass = enhanceMixInClass(originalMixInClass, openLService.getClassLoader());
        }
        objectMapper.addMixIn(sprBeanClass, mixInClass);
    }
}
