package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static org.openl.studio.projects.service.tables.TableTestProjects.projectModel;
import static org.openl.studio.projects.service.tables.TableTestProjects.row;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContextOptimizationForOpenMethodDispatcher;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.studio.common.exception.ConflictException;
import org.openl.types.IOpenMethod;

class DispatchedVersionCheckTest {

    private static final String REFUSAL = "openl.error.409.run.dispatched.compile.errors.message";

    private static ProjectModel module;

    @BeforeAll
    static void compileModule(@TempDir Path dir) throws Exception {
        module = projectModel(dir, "Rules", sheet -> {
            version(sheet, 1, "Rules Double Price(String make)", "make", 2009, "10");
            version(sheet, 12, "Rules Double Price(String make)", "make", 2010, "abc");
            version(sheet, 23, "Rules Double Sound(String make)", "make", 2009, "10");
            version(sheet, 34, "Rules Double Sound(String make)", "make", 2010, "20");
            version(sheet, 45, "Rules Double Injected(String make : context.lob)", "make", 2009, "10");
            version(sheet, 56, "Rules Double Injected(String make : context.lob)", "make", 2010, "abc");
            row(sheet, 67, 1, "Datatype Carrier");
            row(sheet, 68, 1, "String", "lob :context");
            version(sheet, 70, "Rules Double Quote(Carrier carrier)", "carrier.lob", 2009, "10");
            version(sheet, 81, "Rules Double Quote(Carrier carrier)", "carrier.lob", 2010, "abc");
            plain(sheet, 92, "Rules Double Plain(String make)", "10");
            plain(sheet, 99, "Rules Double Broken(String make)", "abc");
        });
        assertNotNull(module.getOpenedModuleCompiledOpenClass());
        assertNotNull(module.getCompiledOpenClass());
    }

    @Test
    void breaksOnlyTheLaterVersionOfEveryOverloadedTable() {
        for (var name : List.of("Price", "Injected", "Quote")) {
            assertTrue(errorsOf(version(name, 2009)).isEmpty(), name);
            assertFalse(errorsOf(version(name, 2010)).isEmpty(), name);
        }
        assertTrue(errorsOf(table("Sound")).isEmpty());
        assertTrue(errorsOf(table("Plain")).isEmpty());
        assertFalse(errorsOf(table("Broken")).isEmpty());
    }

    @Test
    void asksNothingOfARunWithoutARuntimeContext() {
        var broken = version("Price", 2010);

        assertDoesNotThrow(() -> refuse(broken, null, true));
    }

    @Test
    void refusesARunWhoseContextPicksTheVersionThatDoesNotCompile() {
        var requested = version("Price", 2009);
        var context = on(2010, 6, 1);

        var refusal = assertThrows(ConflictException.class, () -> refuse(requested, context, true));

        assertEquals(REFUSAL, refusal.getErrorCode());
        assertEquals(1, refusal.getArgs().length);
        var summary = refusal.getArgs()[0].toString();
        assertTrue(summary.contains("abc"), summary);
        assertFalse(summary.contains("\n"), summary);
    }

    @Test
    void refusesTheSameRunAcrossTheWholeProject() {
        var requested = version("Price", 2009);
        var context = on(2010, 6, 1);

        var refusal = assertThrows(ConflictException.class, () -> refuse(requested, context, false));

        assertEquals(REFUSAL, refusal.getErrorCode());
    }

    @Test
    void refusesWhenTheVersionNamedInTheRequestIsTheOneThatDoesNotCompile() {
        var requested = version("Price", 2010);
        var context = on(2010, 6, 1);

        assertThrows(ConflictException.class, () -> refuse(requested, context, true));
    }

    @Test
    void letsARunPassWhenTheContextPicksTheVersionThatCompilesWhileAnotherDoesNot() {
        var requested = version("Price", 2010);

        assertDoesNotThrow(() -> refuse(requested, on(2009, 6, 1), true));
        assertDoesNotThrow(() -> refuse(requested, on(2009, 6, 1), false));
    }

    @Test
    void letsARunPassWhenEveryVersionCompiles() {
        var requested = version("Sound", 2009);

        assertDoesNotThrow(() -> refuse(requested, on(2010, 6, 1), true));
        assertDoesNotThrow(() -> refuse(requested, on(2009, 6, 1), true));
    }

    @Test
    void letsARunPassWhenNoVersionFitsTheContext() {
        var requested = version("Price", 2009);

        assertDoesNotThrow(() -> refuse(requested, on(2020, 1, 1), true));
    }

    @Test
    void leavesATableWithoutVersionsToTheCheckOfTheTableItself() {
        assertDoesNotThrow(() -> refuse(table("Plain"), on(2010, 6, 1), true));
        assertDoesNotThrow(() -> refuse(table("Broken"), on(2010, 6, 1), true));
    }

    @Test
    void letsARunPassForATableTheModuleDoesNotHold() {
        var absent = mock(IOpenLTable.class);
        when(absent.getUri()).thenReturn("file:/absent.xlsx?sheet=Rules&range=B2:C3");

        assertDoesNotThrow(() -> refuse(absent, on(2010, 6, 1), true));
    }

    @Test
    void refusesBrokenVersionWhenContextIsFilledInFromAParameter() {
        var requested = version("Injected", 2009);

        var context = on(2010, 6, 1);

        assertThrows(ConflictException.class, () -> refuse(requested, context, true));
        assertThrows(ConflictException.class, () -> refuse(requested, context, false));
    }

    @Test
    void staysSilentWhereTheContextIsFilledInFromAFieldOfAParameterBeforeTheVersionIsChosen() {
        var requested = version("Quote", 2009);

        assertDoesNotThrow(() -> refuse(requested, on(2010, 6, 1), true));
        assertDoesNotThrow(() -> refuse(requested, on(2010, 6, 1), false));
    }

    @Test
    void selectsVersionUsingAContextPropertyFromADatatypeField() throws Exception {
        var requested = version("Quote", 2009);
        var dispatcher = module.getOpenedModuleMethod(requested.getUri());
        var carrierClass = module.getOpenedModuleCompiledOpenClass()
                .getOpenClassWithErrors().findType("Carrier").getInstanceClass();
        var carrier = carrierClass.getDeclaredConstructor().newInstance();
        carrierClass.getMethod("setLob", String.class).invoke(carrier, "Banking");

        var selected = select(dispatcher, requested, on(2010, 6, 1), carrier).version();

        assertNotNull(selected);
        assertEquals(version("Quote", 2010).getUri(), selected.getInfo().getSourceUrl());
    }

    @Test
    void readsTheErrorsOfTheOpenedModuleWhenTheRunStaysInIt() {
        var requested = version("Price", 2009);
        var model = spy(module);
        doReturn(List.of()).when(model).getMessagesByTsn(anyString(), eq(Severity.ERROR));
        var context = on(2010, 6, 1);

        assertThrows(ConflictException.class, () -> refuse(model, requested, context, true));
        assertDoesNotThrow(() -> refuse(model, requested, context, false));
    }

    @Test
    void readsTheErrorsOfTheWholeProjectWhenTheRunSpansIt() {
        var requested = version("Price", 2009);
        var model = spy(module);
        doReturn(List.of()).when(model).getOpenedModuleMessagesByTsn(anyString(), eq(Severity.ERROR));
        var context = on(2010, 6, 1);

        assertThrows(ConflictException.class, () -> refuse(model, requested, context, false));
        assertDoesNotThrow(() -> refuse(model, requested, context, true));
    }

    @Test
    void staysSilentWhenTheTableOfTheChosenVersionCannotBeLocated() {
        var requested = version("Price", 2009);
        var model = spy(module);
        doReturn(null).when(model).getTableByUri(anyString());

        assertDoesNotThrow(() -> refuse(model, requested, on(2010, 6, 1), true));
        assertDoesNotThrow(() -> refuse(model, requested, on(2010, 6, 1), false));
    }

    @Test
    void leavesTheContextOfTheRunWithoutTheVersionItChose() {
        var requested = version("Price", 2009);
        var context = on(2009, 6, 1);
        var dispatcher = assertInstanceOf(OpenMethodDispatcher.class, WrapperLogic.unwrapOpenMethod(
                AbstractMethodExecutorService.resolveMethod(module, requested, true, context)));
        var control = on(2009, 6, 1);
        var env = new SimpleRulesVM().getRuntimeEnv();
        env.setContext(control);
        dispatcher.findMatchingMethod(env);
        assertNotNull(chosenFor(control, dispatcher));

        assertDoesNotThrow(() -> refuse(requested, context, true));

        assertNull(chosenFor(context, dispatcher));
    }

    @Test
    void validatesTheRequestedVersionWithoutContextAndTheDispatchedVersionWithContext() {
        var oldTable = version("Sound", 2009);
        var newTable = version("Sound", 2010);
        var dispatcher = module.getOpenedModuleMethod(oldTable.getUri());
        var oldVersion = module.getCurrentDispatcherMethod(dispatcher, oldTable.getUri());

        assertEquals(oldVersion, select(dispatcher, oldTable, null).version());
        assertEquals(newTable.getUri(), select(dispatcher, oldTable, on(2010, 6, 1), "make").version()
                .getInfo().getSourceUrl());
    }

    @Test
    void selectsNoVersionWhenTheContextFitsNone() {
        var table = version("Sound", 2009);
        var dispatcher = module.getOpenedModuleMethod(table.getUri());

        var selection = select(dispatcher, table, on(2020, 1, 1), "make");

        assertNotNull(selection.executable());
        assertNull(selection.version());
    }

    @Test
    void selectsTheRequestedVersionOfATableWithAnInjectedContextUnlessContextIsGiven() {
        var injected = version("Injected", 2009);
        var injectedMethod = module.getOpenedModuleMethod(injected.getUri());
        var requestedVersion = module.getCurrentDispatcherMethod(injectedMethod, injected.getUri());

        assertEquals(requestedVersion, select(injectedMethod, injected, null, "auto").version());
        assertEquals(version("Injected", 2010).getUri(), select(injectedMethod, injected, on(2010, 6, 1), "auto")
                .version().getInfo().getSourceUrl());
    }

    @Test
    void choosingValidationVersionDoesNotCacheItInTheCallerContext() {
        var requested = version("Sound", 2009);
        var context = on(2010, 6, 1);
        var dispatcher = assertInstanceOf(OpenMethodDispatcher.class,
                module.getOpenedModuleMethod(requested.getUri()));

        assertNotNull(select(dispatcher, requested, context, "make").version());
        assertNull(chosenFor(context, dispatcher));
    }

    @Test
    void refusesTheBrokenVersionTheContextPicksWhenTheRequestedTableIsASingleVersion() {
        var requested = version("Price", 2009);
        var dispatcher = module.getOpenedModuleMethod(requested.getUri());
        var single = module.getCurrentDispatcherMethod(dispatcher, requested.getUri());
        var context = on(2010, 6, 1);
        var selection = select(single, requested, context, "make");

        assertThrows(ConflictException.class,
                () -> DispatchedVersionCheck.refuseIfBroken(module, selection, true));
    }

    private static DispatchedVersionCheck.Selection select(IOpenMethod method,
                                                           IOpenLTable table,
                                                           IRulesRuntimeContext context,
                                                           Object... params) {
        return DispatchedVersionCheck.select(module, method, table.getUri(), context, params, true);
    }

    private static IOpenMethod chosenFor(IRulesRuntimeContext context, OpenMethodDispatcher dispatcher) {
        return ((IRulesRuntimeContextOptimizationForOpenMethodDispatcher) context)
                .getMethodForOpenMethodDispatcher(dispatcher);
    }

    private static void refuse(IOpenLTable table, IRulesRuntimeContext context, boolean currentOpenedModule) {
        refuse(module, table, context, currentOpenedModule);
    }

    private static void refuse(ProjectModel model,
                               IOpenLTable table,
                               IRulesRuntimeContext context,
                               boolean currentOpenedModule) {
        var method = currentOpenedModule
                ? model.getOpenedModuleMethod(table.getUri())
                : model.getMethod(table.getUri());
        if (method == null) {
            return;
        }
        var selection = DispatchedVersionCheck.select(model, method, table.getUri(), context, new Object[]{"make"},
                currentOpenedModule);
        DispatchedVersionCheck.refuseIfBroken(model, selection, currentOpenedModule);
    }

    private static List<OpenLMessage> errorsOf(IOpenLTable table) {
        return module.getOpenedModuleMessagesByTsn(table.getUri(), Severity.ERROR);
    }

    private static IRulesRuntimeContext on(int year, int month, int day) {
        var context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCurrentDate(date(year, month, day));
        return context;
    }

    private static Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static IOpenLTable table(String name) {
        return tablesNamed(name).getFirst();
    }

    private static IOpenLTable version(String name, int effectiveYear) {
        return tablesNamed(name).stream()
                .filter(table -> table.getProperties().getEffectiveDate() != null)
                .filter(table -> effectiveYear == effectiveYear(table))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No version of " + name + " from " + effectiveYear));
    }

    private static int effectiveYear(IOpenLTable table) {
        return table.getProperties().getEffectiveDate().toInstant().atZone(ZoneId.systemDefault()).getYear();
    }

    private static List<IOpenLTable> tablesNamed(String name) {
        return module.getAllTableSyntaxNodes()
                .stream()
                .<IOpenLTable>map(TableSyntaxNodeAdapter::new)
                .filter(table -> name.equals(table.getName()))
                .toList();
    }

    private static void version(Sheet sheet,
                                int top,
                                String header,
                                String argument,
                                int year,
                                String price) {
        row(sheet, top, 1, header);
        row(sheet, top + 1, 1, "properties", "effectiveDate");
        row(sheet, top + 2, 2, "expirationDate");
        dateCell(sheet, top + 1, 3, LocalDate.of(year, Month.JANUARY, 1));
        dateCell(sheet, top + 2, 3, LocalDate.of(year + 1, Month.JANUARY, 1));
        sheet.addMergedRegion(new CellRangeAddress(top + 1, top + 2, 1, 1));
        sheet.addMergedRegion(new CellRangeAddress(top, top, 1, 3));
        body(sheet, top + 3, argument, price);
        for (var r = top + 3; r < top + 8; r++) {
            sheet.addMergedRegion(new CellRangeAddress(r, r, 2, 3));
        }
    }

    private static void plain(Sheet sheet, int top, String header, String price) {
        row(sheet, top, 1, header);
        sheet.addMergedRegion(new CellRangeAddress(top, top, 1, 2));
        body(sheet, top + 1, "make", price);
    }

    private static void body(Sheet sheet, int top, String argument, String price) {
        row(sheet, top, 1, "C1", "RET1");
        row(sheet, top + 1, 1, argument + " == c1", "value");
        row(sheet, top + 2, 1, "String c1", "Double value");
        row(sheet, top + 3, 1, "Make", "Price");
        row(sheet, top + 4, 1, "Toyota", price);
    }

    private static void dateCell(Sheet sheet, int rowIndex, int column, LocalDate day) {
        var workbook = sheet.getWorkbook();
        var style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("mm/dd/yyyy"));
        var cell = sheet.getRow(rowIndex).createCell(column);
        cell.setCellValue(date(day.getYear(), day.getMonthValue(), day.getDayOfMonth()));
        cell.setCellStyle(style);
    }
}
