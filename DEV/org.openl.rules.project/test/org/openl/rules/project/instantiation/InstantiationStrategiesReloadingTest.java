package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.util.FileUtils;

@Disabled(value = "currently disabled. Problem with OpenL onstances caching and sharing should be fixed.")
public class InstantiationStrategiesReloadingTest {
    private static final String RULES_ENGINE = "./test-resources/reloading-test/EngineProject/TemplateRules.xls";
    private static final String RULES_API = "./test-resources/reloading-test/SimpleProject/TemplateRules.xls";
    private static final String BEAN_ENGINE = "./test-resources/reloading-test/EngineProject/classes/org/openl/example/TestBean.class";
    private static final String BEAN_API = "./test-resources/reloading-test/SimpleProject/org/openl/example/TestBean.class";
    private static final String SERVICE_CLASS_ENGINE = "./test-resources/reloading-test/EngineProject/classes/org/openl/example/ServiceClass.class";
    private static final String RULES_MODIFIED = "./test-resources/reloading-test/modifications/TemplateRules.xls";
    private static final String BEAN_MODIFIED = "./test-resources/reloading-test/modifications/org/openl/example/TestBean.class";
    private static final String SERVICE_CLASS_MODIFIED = "./test-resources/reloading-test/modifications/org/openl/example/ServiceClass.class";
    private static final String RULES_ORIGINAL = "./test-resources/reloading-test/original/TemplateRules.xls";
    private static final String BEAN_ORIGINAL = "./test-resources/reloading-test/original/org/openl/example/TestBean.class";
    private static final String SERVICE_CLASS_ORIGINAL = "./test-resources/reloading-test/original/org/openl/example/ServiceClass.class";

    private static final MethodDescription GET_STRING_FIELD = new MethodDescription("getStringField",
            null,
            String.class);
    private static final MethodDescription INVOKE = new MethodDescription("invoke", null, String.class);
    private static final MethodDescription GET_INT_FIELD = new MethodDescription("getIntField", null, int.class);

    private static MethodDescription getGetIntMethod(RulesInstantiationStrategy strategy) throws Exception {
        return new MethodDescription("getInt",
                new Class[]{strategy.getClassLoader().loadClass("org.openl.example.TestBean")},
                int.class);
    }

    private static final ProjectResolver resolver = ProjectResolver.getInstance();
    private ApiBasedInstantiationStrategy apiStrategy;
    private ApiBasedInstantiationStrategy dynamicStrategy;

    private static ApiBasedInstantiationStrategy resolve(File folder) throws Exception {
        ProjectDescriptor project = resolver.resolve(folder);
        if (project != null) {
            List<PathEntry> classpath = project.getClasspath();
            if (classpath == null) {
                classpath = new ArrayList<>();
                project.setClasspath(classpath);
            }
            return new ApiBasedInstantiationStrategy(project.getModules().get(0), null, null, false);
        } else {
            throw new RuntimeException("Wrong folder.");
        }
    }

    @BeforeEach
    public void init() throws Exception {
        apiStrategy = resolve(
                new File("./test-resources/reloading-test/SimpleProject"));
        dynamicStrategy = resolve(new File("./test-resources/reloading-test/EngineProject"));
    }

    public void checkOriginal(Object instance) throws Exception {
        Method invokeMethod = instance.getClass().getMethod("invoke");
        assertEquals(invokeMethod.invoke(instance), "original");
    }

    public void checkModified(Object instance) throws Exception {
        Method invokeMethod = instance.getClass().getMethod("invoke");
        assertEquals(invokeMethod.invoke(instance), "modified");
    }

    public void checkClass(Class<?> classToCheck,
                           MethodDescription[] shouldBeRepresented,
                           MethodDescription[] shouldNotBeRepresented) throws Exception {
        for (MethodDescription method : shouldBeRepresented) {
            Method methodRepresented = classToCheck.getMethod(method.getName(), method.getParamTypes());
            assertNotNull(methodRepresented);
            assertEquals(methodRepresented.getReturnType(), method.getReturnType());
        }
        for (MethodDescription method : shouldNotBeRepresented) {
            try {
                assertNull(classToCheck.getMethod(method.getName(), method.getParamTypes()));
            } catch (NoSuchMethodException e) {
                assertTrue(true);
            }
        }
    }

    public void checkClass(String className,
                           RulesInstantiationStrategy strategy,
                           MethodDescription[] shouldBeRepresented,
                           MethodDescription[] shouldNotBeRepresented) throws Exception {
        Class<?> clazz = strategy.getClassLoader().loadClass(className);
        checkClass(clazz, shouldBeRepresented, shouldNotBeRepresented);
    }

    @SuppressWarnings("rawtypes")
    public static class MethodDescription {
        private final String name;
        private final Class[] paramTypes;
        private final Class<?> returnType;

        public MethodDescription(String name, Class[] paramTypes, Class<?> returnType) {
            this.name = name;
            this.paramTypes = paramTypes == null ? new Class[0] : paramTypes;
            this.returnType = returnType;
        }

        public String getName() {
            return name;
        }

        public Class[] getParamTypes() {
            return paramTypes;
        }

        public Class<?> getReturnType() {
            return returnType;
        }
    }

    @AfterEach
    public void restoreChanges() throws IOException {
        System.out.println("Restoring changes...");
        FileUtils.copy(new File(RULES_ORIGINAL), new File(RULES_ENGINE));
        FileUtils.copy(new File(RULES_ORIGINAL), new File(RULES_API));
        FileUtils.copy(new File(BEAN_ORIGINAL), new File(BEAN_ENGINE));
        FileUtils.copy(new File(BEAN_ORIGINAL), new File(BEAN_API));
        FileUtils.copy(new File(SERVICE_CLASS_ORIGINAL), new File(SERVICE_CLASS_ENGINE));
        System.out.println("All files have been successfully restored.");
    }

    public void makeChanges() throws IOException {
        System.out.println("Modifying files...");
        FileUtils.copy(new File(RULES_MODIFIED), new File(RULES_ENGINE));
        FileUtils.copy(new File(RULES_MODIFIED), new File(RULES_API));
        FileUtils.copy(new File(BEAN_MODIFIED), new File(BEAN_ENGINE));
        FileUtils.copy(new File(BEAN_MODIFIED), new File(BEAN_API));
        FileUtils.copy(new File(SERVICE_CLASS_MODIFIED), new File(SERVICE_CLASS_ENGINE));
        System.out.println("All files have been successfully changed.");
    }

    @Test
    public void testSimpleReset() throws Exception {
        checkOriginal(apiStrategy.instantiate());
        checkClass(apiStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(apiStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                apiStrategy,
                new MethodDescription[]{GET_INT_FIELD, GET_STRING_FIELD},
                new MethodDescription[0]);
        checkOriginal(dynamicStrategy.instantiate());
        checkClass(dynamicStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(dynamicStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                dynamicStrategy,
                new MethodDescription[]{GET_INT_FIELD, GET_STRING_FIELD},
                new MethodDescription[0]);
        makeChanges();
        apiStrategy.reset();
        checkModified(apiStrategy.instantiate());
        checkClass(apiStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(apiStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                apiStrategy,
                new MethodDescription[]{GET_INT_FIELD, GET_STRING_FIELD},
                new MethodDescription[0]);
        dynamicStrategy.reset();
        checkModified(dynamicStrategy.instantiate());
        checkClass(dynamicStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(dynamicStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                dynamicStrategy,
                new MethodDescription[]{GET_INT_FIELD, GET_STRING_FIELD},
                new MethodDescription[0]);
    }

    @Test
    public void testForsedReset() throws Exception {
        checkOriginal(apiStrategy.instantiate());
        checkClass(apiStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(apiStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                apiStrategy,
                new MethodDescription[]{GET_INT_FIELD, GET_STRING_FIELD},
                new MethodDescription[0]);
        checkOriginal(dynamicStrategy.instantiate());
        checkClass(dynamicStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(dynamicStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                dynamicStrategy,
                new MethodDescription[]{GET_INT_FIELD, GET_STRING_FIELD},
                new MethodDescription[0]);
        makeChanges();
        apiStrategy.forcedReset();
        checkModified(apiStrategy.instantiate());
        checkClass(apiStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE, getGetIntMethod(apiStrategy)},
                new MethodDescription[0]);
        checkClass("org.openl.example.TestBean",
                apiStrategy,
                new MethodDescription[]{GET_INT_FIELD},
                new MethodDescription[]{GET_STRING_FIELD});
        dynamicStrategy.forcedReset();
        checkModified(dynamicStrategy.instantiate());
        checkClass(dynamicStrategy.getInstanceClass(),
                new MethodDescription[]{INVOKE},
                new MethodDescription[]{getGetIntMethod(dynamicStrategy)});
        checkClass("org.openl.example.TestBean",
                dynamicStrategy,
                new MethodDescription[]{GET_INT_FIELD},
                new MethodDescription[]{GET_STRING_FIELD});
    }
}
