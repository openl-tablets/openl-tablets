package org.openl.rules.openapi.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.model.scaffolding.PathInfo;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetModel;
import org.openl.rules.model.scaffolding.StepModel;
import org.openl.rules.model.scaffolding.TypeInfo;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.RulesType;

import groovy.lang.GroovyClassLoader;

public class OpenAPIGroovyScriptGeneratorTest {

    private OpenAPIModelConverter converter;
    private GroovyClassLoader groovyClassLoader;

    @Before
    public void setUp() {
        converter = new OpenAPIScaffoldingConverter();
        groovyClassLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testOpenAPIEmpty() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapiNothingToGenerate.yaml");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        assertNull(generated.getAnnotationTemplateGroovyFile());
        assertTrue(generated.getGroovyCommonClasses().isEmpty());
    }

    @Test
    public void testOpenAPIPathInfo() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/slashProblem.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();

        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);

        assertEquals(2, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("apiTodo", Integer.class);
        assertEquals(Integer.class, method1.getReturnType());

        assertEquals(4, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/api/Todo", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "text/csv" }, method1.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "text/html" }, method1.getAnnotation(Produces.class).value());
        assertEquals(0, method1.getParameters()[0].getDeclaredAnnotations().length);

        Method method2 = interfaceClass.getDeclaredMethod("apiBla", Integer.class);
        assertEquals(SpreadsheetResult.class, method2.getReturnType());

        assertEquals(4, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));
        assertEquals("/api/Bla", method2.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" }, method2.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "text/plain" }, method2.getAnnotation(Produces.class).value());
        assertEquals(0, method2.getParameters()[0].getDeclaredAnnotations().length);
    }

    @Test
    public void testOpenAPIJavaInterfaceGenerator() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapi.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();

        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);

        assertEquals(4, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("apipolicyProxy2", Object[].class);
        assertEquals(Object[].class, method1.getReturnType());
        assertEquals(5, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/api/policyProxy2", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Produces.class).value());
        assertEquals("Policy", method1.getAnnotation(RulesType.class).value());

        assertEquals(1, method1.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method1.getParameters()[0].getAnnotation(RulesType.class).value());

        Method method2 = interfaceClass.getDeclaredMethod("apipolicyProxy3", Object.class, Object.class);
        assertEquals(Object[].class, method2.getReturnType());
        assertEquals(5, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));
        assertEquals("/api/policyProxy3", method2.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" }, method2.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "application/json" }, method2.getAnnotation(Produces.class).value());
        assertEquals("Policy", method2.getAnnotation(RulesType.class).value());

        assertEquals(1, method2.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method2.getParameters()[0].getAnnotation(RulesType.class).value());
        assertEquals(1, method2.getParameters()[1].getAnnotations().length);
        assertEquals("Policy", method2.getParameters()[1].getAnnotation(RulesType.class).value());

        Method method3 = interfaceClass.getDeclaredMethod("apipolicyProxy", Object.class);
        assertEquals(Object.class, method3.getReturnType());
        assertEquals(5, method3.getDeclaredAnnotations().length);
        assertNotNull(method3.getAnnotation(POST.class));
        assertEquals("/api/policyProxy", method3.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" }, method3.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "application/json" }, method3.getAnnotation(Produces.class).value());
        assertEquals("Policy", method3.getAnnotation(RulesType.class).value());

        assertEquals(1, method3.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method3.getParameters()[0].getAnnotation(RulesType.class).value());

        Method method4 = interfaceClass.getDeclaredMethod("apidoSomething", Object.class);
        assertEquals(SpreadsheetResult.class, method4.getReturnType());
        assertEquals(4, method4.getDeclaredAnnotations().length);
        assertNotNull(method4.getAnnotation(POST.class));
        assertEquals("/api/doSomething", method4.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" }, method4.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "application/json" }, method4.getAnnotation(Produces.class).value());

        assertEquals(1, method4.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method4.getParameters()[0].getAnnotation(RulesType.class).value());
    }

    @Test
    public void testOpenAPIJavaInterfaceGeneratorPathParam() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapiPathParam.yaml");
        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        Set<Class<?>> commonClasses = new HashSet<>();
        for (GroovyScriptFile groovyScriptFile : generated.getGroovyCommonClasses()) {
            Class<?> clazz = defineClass(groovyScriptFile.getScriptText());
            assertJavaClass(groovyScriptFile.getNameWithPackage(), clazz);
            if (!commonClasses.add(clazz)) {
                fail("Duplicated class!");
            }
        }
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);

        Method method1 = interfaceClass.getDeclaredMethod("pet", long.class);
        assertEquals(Object.class, method1.getReturnType());
        assertEquals(4, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(GET.class));
        assertEquals("/pet/{petId}", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "text/plain" }, method1.getAnnotation(Produces.class).value());
        assertEquals("Pet", method1.getAnnotation(RulesType.class).value());

        assertEquals(1, method1.getParameters()[0].getAnnotations().length);
        assertEquals("petId", method1.getParameters()[0].getAnnotation(PathParam.class).value());
    }

    @Test
    public void testOpenAPIJavaInterfaceGeneratorExtraMeth() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapiOnlyExtraMethod.json");
        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        Set<Class<?>> commonClasses = new HashSet<>();
        for (GroovyScriptFile groovyScriptFile : generated.getGroovyCommonClasses()) {
            Class<?> clazz = defineClass(groovyScriptFile.getScriptText());
            assertJavaClass(groovyScriptFile.getNameWithPackage(), clazz);
            if (!commonClasses.add(clazz)) {
                fail("Duplicated class!");
            }
        }
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);

        assertEquals(1, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("policyProxy", Object.class, Object.class);
        assertEquals(Object.class, method1.getReturnType());
        assertEquals(6, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/policyProxy", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Consumes.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Produces.class).value());
        assertEquals("Policy", method1.getAnnotation(RulesType.class).value());
        // assertTrue(commonClasses.contains(method1.getAnnotation(ServiceExtraMethod.class).value()));

        assertEquals(2, method1.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method1.getParameters()[0].getAnnotation(RulesType.class).value());
        assertEquals("policy", method1.getParameters()[0].getAnnotation(Name.class).value());

        assertEquals(2, method1.getParameters()[1].getAnnotations().length);
        assertEquals("Coverage", method1.getParameters()[1].getAnnotation(RulesType.class).value());
        assertEquals("coverage", method1.getParameters()[1].getAnnotation(Name.class).value());
    }

    @Test
    public void testOpenAPIJavaInterfaceGeneratorRuntimeContext() throws Exception {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/paths/runtimeContextAndExtraMethod.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        Set<Class<?>> commonClasses = new HashSet<>();
        for (GroovyScriptFile groovyScriptFile : generated.getGroovyCommonClasses()) {
            Class<?> clazz = defineClass(groovyScriptFile.getScriptText());
            assertJavaClass(groovyScriptFile.getNameWithPackage(), clazz);
            if (!commonClasses.add(clazz)) {
                fail("Duplicated class!");
            }
        }
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);

        assertEquals(2, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass
            .getDeclaredMethod("apiBla", Integer.class, String.class, Boolean.class, Boolean.class);
        assertEquals(Integer.class, method1.getReturnType());
        assertEquals(4, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/apiBla", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "text/plain" }, method1.getAnnotation(Produces.class).value());
        assertTrue(commonClasses.contains(method1.getAnnotation(ServiceExtraMethod.class).value()));

        assertEquals(1, method1.getParameters()[0].getAnnotations().length);
        assertEquals("id", method1.getParameters()[0].getAnnotation(Name.class).value());
        assertEquals(1, method1.getParameters()[1].getAnnotations().length);
        assertEquals("name", method1.getParameters()[1].getAnnotation(Name.class).value());
        assertEquals(1, method1.getParameters()[2].getAnnotations().length);
        assertEquals("isCompleted", method1.getParameters()[2].getAnnotation(Name.class).value());
        assertEquals(1, method1.getParameters()[3].getAnnotations().length);
        assertEquals("someStep", method1.getParameters()[3].getAnnotation(Name.class).value());

        Method method2 = interfaceClass.getDeclaredMethod("apiTodo1", IRulesRuntimeContext.class);
        assertEquals(Integer.class, method2.getReturnType());
        assertEquals(4, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));
        assertEquals("/api/Todo", method2.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "text/plain" }, method2.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "application/json" }, method2.getAnnotation(Consumes.class).value());
        assertEquals(0, method2.getParameters()[0].getAnnotations().length);
    }

    @Test
    public void EPBDS_10493() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapi_EPBDS-10493.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();

        Set<Class<?>> commonClasses = new HashSet<>();
        for (GroovyScriptFile groovyScriptFile : generated.getGroovyCommonClasses()) {
            Class<?> clazz = defineClass(groovyScriptFile.getScriptText());
            assertJavaClass(groovyScriptFile.getNameWithPackage(), clazz);
            if (!commonClasses.add(clazz)) {
                fail("Duplicated class!");
            }
        }
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);

        assertEquals(1, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("DiscountPercentage", Object.class, Integer.class);
        assertEquals(Double.class, method1.getReturnType());
        assertEquals(5, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/DiscountPercentage", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "text/plain" }, method1.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Consumes.class).value());
        assertTrue(commonClasses.contains(method1.getAnnotation(ServiceExtraMethod.class).value()));
    }

    @Test
    public void EPBDS_10962() throws Exception {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/paths/openapi_EPBDS-10962_generate.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);
        assertEquals(1, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("mySpr3", double.class, double.class, double.class);
        assertEquals(Double.class, method1.getReturnType());

        assertEquals(3, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(GET.class));
        assertEquals("/mySpr3/{double1}/{double2}/{double3}/{double4}", method1.getAnnotation(Path.class).value());
        assertNull(method1.getAnnotation(Consumes.class));
        assertArrayEquals(new String[] { "text/plain" }, method1.getAnnotation(Produces.class).value());
        assertEquals(1, method1.getParameters()[0].getDeclaredAnnotations().length);

        ProjectModel projectModel2 = converter
            .extractProjectModel("test.converter/paths/openapi_EPBDS-10962_nothingToGenerate.json");

        OpenAPIGeneratedClasses generated2 = new OpenAPIJavaClassGenerator(projectModel2).generate();
        assertNull(generated2.getAnnotationTemplateGroovyFile());
        assertTrue(generated2.getGroovyCommonClasses().isEmpty());
    }

    @Test
    public void resolveTypeTest() {
        TypeInfo typeInfo = new TypeInfo("Policy", "Policy", TypeInfo.Type.DATATYPE);
        assertEquals(Object.class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));

        typeInfo.setDimension(1);
        assertEquals(Object[].class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));

        typeInfo.setDimension(3);
        assertEquals(Object[][][].class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));

        typeInfo = new TypeInfo(Integer.class);
        assertEquals(Integer.class.getName(), OpenAPIJavaClassGenerator.resolveType(typeInfo));
    }

    @Test
    public void test_EPBDS_10988() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/EPBDS-10988_OpenAPI.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);
        assertEquals(1, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("getTestData1", IRulesRuntimeContext.class, String.class);
        assertEquals(Double.class, method1.getReturnType());
        assertEquals(4, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/getTestData1", method1.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "text/plain" }, method1.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Consumes.class).value());
        assertEquals(1, method1.getParameters()[0].getAnnotations().length);
        assertEquals("param0", method1.getParameters()[0].getAnnotation(Name.class).value());
        assertEquals(0, method1.getParameters()[1].getAnnotations().length);
    }

    @Test
    public void test_mustNotGenerateInterface() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/openapi_defaultContext.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        assertNull(generated.getAnnotationTemplateGroovyFile());
        assertTrue(generated.getGroovyCommonClasses().isEmpty());
    }

    @Test
    public void test_DataTables() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/openapi_EPBDS-10993.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        assertNull(generated.getAnnotationTemplateGroovyFile());
        assertTrue(generated.getGroovyCommonClasses().isEmpty());
    }

    @Test
    public void test_DataTables2() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/data_tables/openapi_dataTables.json");

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        GroovyScriptFile groovyFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyFile.getScriptText());
        assertInterfaceDescription(groovyFile.getNameWithPackage(), interfaceClass);
        assertEquals(1, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("getPolicyData", IRulesRuntimeContext.class);
        assertEquals(Object[].class, method1.getReturnType());
        assertEquals(5, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/getPolicyData", method1.getAnnotation(Path.class).value());
        assertEquals("Policy", method1.getAnnotation(RulesType.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "multipart/form-data" }, method1.getAnnotation(Consumes.class).value());
        assertEquals(0, method1.getParameters()[0].getAnnotations().length);
    }

    @Test
    public void test_dataTablesAndRuntimeContextAndExtraMethod() throws Exception {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/data_tables/openapi_dataTablesAndRuntimeContextAndExtraMethod.json");
        assertSetEquals(toSet(".+ getPolicyData\\(.*\\)"), projectModel.getIncludeMethodFilter());

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        Set<Class<?>> commonClasses = new HashSet<>();
        for (GroovyScriptFile groovyScriptFile : generated.getGroovyCommonClasses()) {
            Class<?> clazz = defineClass(groovyScriptFile.getScriptText());
            assertJavaClass(groovyScriptFile.getNameWithPackage(), clazz);
            if (!commonClasses.add(clazz)) {
                fail("Duplicated class!");
            }
        }
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);
        assertEquals(2, interfaceClass.getDeclaredMethods().length);

        Method method1 = interfaceClass.getDeclaredMethod("getPolicyData", IRulesRuntimeContext.class);
        assertEquals(Object[].class, method1.getReturnType());
        assertEquals(5, method1.getDeclaredAnnotations().length);
        assertNotNull(method1.getAnnotation(POST.class));
        assertEquals("/getPolicyData", method1.getAnnotation(Path.class).value());
        assertEquals("Policy", method1.getAnnotation(RulesType.class).value());
        assertArrayEquals(new String[] { "application/json" }, method1.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "multipart/form-data" }, method1.getAnnotation(Consumes.class).value());
        assertEquals(0, method1.getParameters()[0].getAnnotations().length);

        Method method2 = interfaceClass.getDeclaredMethod("spr", Object.class);
        assertEquals(Object.class, method2.getReturnType());
        assertEquals(6, method2.getDeclaredAnnotations().length);
        assertNotNull(method2.getAnnotation(POST.class));
        assertEquals("/spr", method2.getAnnotation(Path.class).value());
        assertEquals("Policy", method2.getAnnotation(RulesType.class).value());
        assertArrayEquals(new String[] { "application/json" }, method2.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "application/json" }, method2.getAnnotation(Consumes.class).value());
        assertEquals(2, method2.getParameters()[0].getAnnotations().length);
        assertEquals("Policy", method2.getParameters()[0].getAnnotation(RulesType.class).value());
        assertEquals("policy", method2.getParameters()[0].getAnnotation(Name.class).value());
        // assertTrue(commonClasses.contains(method2.getAnnotation(ServiceExtraMethod.class).value()));
    }

    @Test
    public void test_nameConflictTest() throws Exception {
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/problems/EPBDS-10995_name_conflict.json");
        assertSetEquals(toSet(".+ MyLovelySpreadsheet\\(.+\\)", ".+ Party\\(.+\\)"),
            projectModel.getIncludeMethodFilter());
        List<SpreadsheetModel> spreadsheetResultModels = projectModel.getSpreadsheetResultModels();
        Optional<SpreadsheetModel> optionalVM = spreadsheetResultModels.stream()
            .filter(model -> model.getName().equals("MyLovelySpreadsheet"))
            .findFirst();
        assertTrue(optionalVM.isPresent());
        SpreadsheetModel validationMessage = optionalVM.get();
        assertEquals(4, validationMessage.getSteps().size());
        assertEquals("SpreadsheetResult", validationMessage.getType());
        Optional<SpreadsheetModel> optionalParty = spreadsheetResultModels.stream()
            .filter(spreadsheetModel -> spreadsheetModel.getName().equals("Party"))
            .findFirst();
        assertTrue(optionalParty.isPresent());
        SpreadsheetModel party = optionalParty.get();
        assertEquals("SpreadsheetResultMyLovelySpreadsheet[]", party.getType());
        PathInfo pathInfo = party.getPathInfo();
        assertEquals("[Lorg.openl.rules.calc.SpreadsheetResult;", pathInfo.getReturnType().getJavaName());
        List<StepModel> steps = party.getSteps();
        assertEquals(1, steps.size());
        StepModel resultStep = steps.iterator().next();
        assertEquals("= new SpreadsheetResultMyLovelySpreadsheet[]{MyLovelySpreadsheet(null)}", resultStep.getValue());
        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        GroovyScriptFile groovyScriptFile = generated.getAnnotationTemplateGroovyFile();
        Class<?> interfaceClass = defineClass(groovyScriptFile.getScriptText());
        assertInterfaceDescription(groovyScriptFile.getNameWithPackage(), interfaceClass);
        assertEquals(1, interfaceClass.getDeclaredMethods().length);

        Method validationMessageMethod = interfaceClass.getDeclaredMethod("MyLovelySpreadsheet", Object.class);
        assertEquals(SpreadsheetResult.class, validationMessageMethod.getReturnType());
        assertEquals(4, validationMessageMethod.getDeclaredAnnotations().length);
        assertNotNull(validationMessageMethod.getAnnotation(POST.class));
        assertEquals("/Watch", validationMessageMethod.getAnnotation(Path.class).value());
        assertArrayEquals(new String[] { "application/json" },
            validationMessageMethod.getAnnotation(Produces.class).value());
        assertArrayEquals(new String[] { "application/json" },
            validationMessageMethod.getAnnotation(Consumes.class).value());
        assertEquals(1, validationMessageMethod.getParameters()[0].getAnnotations().length);
        assertEquals("Watch", validationMessageMethod.getParameters()[0].getAnnotation(RulesType.class).value());
    }

    @Test
    public void test_EPBDS_10979() throws Exception {
        ProjectModel projectModel = converter.extractProjectModel("test.converter/problems/EPBDS-10979_extraSpr.json");
        assertSetEquals(toSet(".+ PlanDetails\\(.+\\)"), projectModel.getIncludeMethodFilter());

        OpenAPIGeneratedClasses generated = new OpenAPIJavaClassGenerator(projectModel).generate();
        assertNull(generated.getAnnotationTemplateGroovyFile());
        assertTrue(generated.getGroovyCommonClasses().isEmpty());
    }

    private static void assertInterfaceDescription(String expectedName, Class<?> interfaceClass) {
        assertNotNull(interfaceClass);
        assertTrue(interfaceClass.isInterface());
        assertTrue("Interface must be public", (interfaceClass.getModifiers() & Modifier.PUBLIC) != 0);
        assertEquals(expectedName, interfaceClass.getName());
    }

    private Class<?> defineClass(String text) {

        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(groovyClassLoader);
            return groovyClassLoader.parseClass(text);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private static void assertJavaClass(String expectedName, Class<?> clazz) throws IllegalAccessException,
                                                                             InstantiationException {

        assertFalse(clazz.isInterface());
        assertTrue("Class must be public", (clazz.getModifiers() & Modifier.PUBLIC) != 0);
        assertEquals(expectedName, clazz.getName());
        clazz.newInstance();// just for sure
    }

    private static void assertSetEquals(Set<String> expected, Set<String> actual) {
        Set<String> rest = new HashSet<>(actual);
        rest.removeAll(expected);
        if (!rest.isEmpty()) {
            fail(String.format("Unexpected items: %s", String.join(", ", rest)));
        }

        rest = new HashSet<>(expected);
        rest.removeAll(actual);
        if (!rest.isEmpty()) {
            fail(String.format("Missed expected items: %s", String.join(", ", rest)));
        }
    }

    private static Set<String> toSet(String... args) {
        return Stream.of(args).collect(Collectors.toSet());
    }
}
