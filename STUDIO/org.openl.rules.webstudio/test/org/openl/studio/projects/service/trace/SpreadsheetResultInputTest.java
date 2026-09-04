package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * A table that takes another spreadsheet's result as an argument must receive it filled from the request JSON.
 *
 * <p>A spreadsheet result has no properties of its own to read into, so the value is read through the bean class
 * OpenL Rule Services publishes for that spreadsheet — the same body a deployed service accepts — and then turned
 * back into a spreadsheet result. Before that detour the argument arrived empty and the rule computed on nothing.
 *
 * <p>Fixture {@code sprParamProject.xlsx}: {@code RatingDetails} produces {@code Plan}, {@code Coverages} and
 * {@code Total}; {@code PolicyOverrideCalculation}, {@code OverrideWithPolicy} and {@code TotalOfAll} take that
 * result as a single argument, as the second of two, and as an array; {@code AnyInput} takes the bare
 * {@code SpreadsheetResult}, which stands for any spreadsheet of the module.
 */
class SpreadsheetResultInputTest {

    private static final String SRC = "test/rules/trace-debug/sprParamProject.xlsx";

    private static XlsModuleOpenClass module;
    private static IOpenClass ratingDetailsType;
    private static ObjectMapper mapper;

    private final TableInputParserServiceImpl parser = new TableInputParserServiceImpl();

    @BeforeAll
    static void compile() throws ClassNotFoundException {
        module = (XlsModuleOpenClass) new RulesEngineFactory<>(SRC).getCompiledOpenClass().getOpenClass();
        ratingDetailsType = module.findType("SpreadsheetResultRatingDetails");
        var factory = new ProjectJacksonObjectMapperFactoryBean();
        factory.setXlsModuleOpenClass(module);
        mapper = factory.createJacksonObjectMapper();
    }

    private static IOpenMethod method(String name, IOpenClass... parameterTypes) {
        var method = module.getMethod(name, parameterTypes);
        assertNotNull(method, name);
        return method;
    }

    private static SpreadsheetResult firstParam(TableInputParserService.ParseResult result) {
        return assertInstanceOf(SpreadsheetResult.class, result.params()[0]);
    }

    /** Runs the rule so the assertions cover the value the engine actually sees, not only the parsed object. */
    private static Object invoke(IOpenMethod method, Object... params) {
        var env = new SimpleRulesVM().getRuntimeEnv();
        return method.invoke(module.newInstance(env), params, env);
    }

    @Test
    @DisplayName("A spreadsheet result argument is filled from the request body of the deployed service")
    void spreadsheetResultArgumentIsMaterialized() {
        var target = method("PolicyOverrideCalculation", ratingDetailsType);

        var result = parser.parseInput(
                "{\"Plan\":\"P1\",\"Total\":7.0,\"Coverages\":[{\"code\":\"C1\",\"premium\":3.0}]}", target, mapper);

        var ratingDetails = firstParam(result);
        assertEquals("P1", ratingDetails.getFieldValue("$Plan"));
        assertEquals(7.0, ratingDetails.getFieldValue("$Total"));
        assertEquals(1, ((Object[]) ratingDetails.getFieldValue("$Coverages")).length);

        var calculated = assertInstanceOf(SpreadsheetResult.class, invoke(target, ratingDetails));
        assertEquals(14.0, calculated.getFieldValue("$Doubled"));
        assertEquals("P1", calculated.getFieldValue("$PlanCode"));
    }

    @Test
    @DisplayName("The name-wrapped form fills the spreadsheet result argument by parameter name")
    void nameWrappedFormFillsSpreadsheetResultArgument() {
        var target = method("PolicyOverrideCalculation", ratingDetailsType);

        var result = parser.parseInput("{\"ratingDetails\":{\"Plan\":\"P2\",\"Total\":5.0}}", target, mapper);

        assertEquals("P2", firstParam(result).getFieldValue("$Plan"));
    }

    @Test
    @DisplayName("The structured form fills the spreadsheet result argument by parameter name")
    void structuredFormFillsSpreadsheetResultArgument() {
        var target = method("PolicyOverrideCalculation", ratingDetailsType);

        var result = parser.parseInput(
                "{\"params\":{\"ratingDetails\":{\"Plan\":\"P3\",\"Total\":9.0}}}", target, mapper);

        assertEquals(9.0, firstParam(result).getFieldValue("$Total"));
    }

    @Test
    @DisplayName("A spreadsheet result argument alongside a plain one is filled too")
    void spreadsheetResultArgumentAmongOthers() {
        var target = method("OverrideWithPolicy", JavaOpenClass.STRING, ratingDetailsType);

        var result = parser.parseInput(
                "{\"policyId\":\"POL-1\",\"ratingDetails\":{\"Plan\":\"P4\",\"Total\":4.0}}", target, mapper);

        assertEquals("POL-1", result.params()[0]);
        var ratingDetails = assertInstanceOf(SpreadsheetResult.class, result.params()[1]);
        assertEquals(4.0, ratingDetails.getFieldValue("$Total"));

        var calculated = assertInstanceOf(SpreadsheetResult.class, invoke(target, "POL-1", ratingDetails));
        assertEquals(5.0, calculated.getFieldValue("$Sum"));
    }

    @Test
    @DisplayName("An array of spreadsheet results is filled element by element")
    void spreadsheetResultArrayArgumentIsMaterialized() {
        var target = method("TotalOfAll", ratingDetailsType.getArrayType(1));

        var result = parser.parseInput("{\"details\":[{\"Total\":1.0},{\"Total\":2.0}]}", target, mapper);

        var details = assertInstanceOf(SpreadsheetResult[].class, result.params()[0]);
        assertEquals(2, details.length);
        assertEquals(2.0, details[1].getFieldValue("$Total"));

        var calculated = assertInstanceOf(SpreadsheetResult.class, invoke(target, (Object) details));
        assertEquals(3.0, calculated.getFieldValue("$Sum"));
    }

    @Test
    @DisplayName("An argument declared as the bare SpreadsheetResult is filled too")
    void anySpreadsheetResultArgumentIsMaterialized() {
        var target = method("AnyInput", module.getSpreadsheetResultOpenClassWithResolvedFieldTypes());

        var result = parser.parseInput("{\"any\":{\"Total\":8.0}}", target, mapper);

        assertEquals(8.0, firstParam(result).getFieldValue("$Total"));
    }

    @Test
    @DisplayName("One argument can be read on its own, as the legacy input form reads it")
    void parseParameterReadsOneArgument() throws Exception {
        var ratingDetails = assertInstanceOf(SpreadsheetResult.class,
                parser.parseParameter("{\"Plan\":\"P6\",\"Total\":3.0}", ratingDetailsType, mapper));

        assertEquals("P6", ratingDetails.getFieldValue("$Plan"));
        assertEquals(3.0, ratingDetails.getFieldValue("$Total"));
    }

    @Test
    @DisplayName("A runtime context survives the round trip alongside the arguments")
    void runtimeContextRoundTrips() throws Exception {
        var target = method("PolicyOverrideCalculation", ratingDetailsType);
        var parsed = parser.parseInput("{\"Plan\":\"P7\",\"Total\":2.0}", target, mapper);
        var context = new DefaultRulesRuntimeContext();
        context.setLob("Home");

        var json = parser.formatInput(parsed.params(), context, target, mapper);
        var reparsed = parser.parseInput(json, target, mapper);

        assertEquals("P7", firstParam(reparsed).getFieldValue("$Plan"));
        assertNotNull(reparsed.runtimeContext());
        assertEquals("Home", reparsed.runtimeContext().getLob());
    }

    @Test
    @DisplayName("A runtime context sent next to the arguments is read by its own name")
    void explicitRuntimeContextFieldIsRead() {
        var target = method("OverrideWithPolicy", JavaOpenClass.STRING, ratingDetailsType);

        var result = parser.parseInput(
                "{\"policyId\":\"POL-3\",\"ratingDetails\":{\"Total\":1.0},\"runtimeContext\":{\"lob\":\"Auto\"}}",
                target, mapper);

        assertEquals(1.0, assertInstanceOf(SpreadsheetResult.class, result.params()[1]).getFieldValue("$Total"));
        assertNotNull(result.runtimeContext());
        assertEquals("Auto", result.runtimeContext().getLob());
    }

    @Test
    @DisplayName("Formatted input is read back into the same spreadsheet result")
    void formattedInputRoundTrips() throws Exception {
        var target = method("PolicyOverrideCalculation", ratingDetailsType);
        var parsed = parser.parseInput("{\"Plan\":\"P5\",\"Total\":6.0}", target, mapper);

        var json = parser.formatInput(parsed.params(), null, target, mapper);
        var reparsed = parser.parseInput(json, target, mapper);

        assertEquals("P5", firstParam(reparsed).getFieldValue("$Plan"));
        assertEquals(6.0, firstParam(reparsed).getFieldValue("$Total"));
    }

    @Test
    @DisplayName("A missing argument is formatted as null and stays null when read back")
    void missingArgumentRoundTripsAsNull() throws Exception {
        var target = method("OverrideWithPolicy", JavaOpenClass.STRING, ratingDetailsType);

        var json = parser.formatInput(new Object[]{"POL-2", null}, null, target, mapper);
        var reparsed = parser.parseInput(json, target, mapper);

        assertEquals("POL-2", reparsed.params()[0]);
        assertNull(reparsed.params()[1]);
    }
}
