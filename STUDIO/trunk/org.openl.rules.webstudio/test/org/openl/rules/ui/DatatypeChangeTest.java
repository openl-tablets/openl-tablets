package org.openl.rules.ui;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.tika.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openl.config.ConfigurationManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class DatatypeChangeTest {
    private static final String RULES_PATH = "test/rules/datatype.change/";
    private static final String RULES_FILE_NAME = "test/rules/datatype.change/DatatypeChange.xls";

    private ProjectModel pm;
    private Module moduleInfo;

    @Before
    public void init() throws Exception {
        File rulesFolder = new File(RULES_PATH);
        ResolvingStrategy resolvingStrategy = RulesProjectResolver.loadProjectResolverFromClassPath().isRulesProject(
                rulesFolder);
        List<Module> modules = resolvingStrategy.resolveProject(rulesFolder).getModules();
        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        List<IDependencyLoader> dependencyLoaders = new ArrayList<IDependencyLoader>(1);
        dependencyLoaders.add(new RulesModuleDependencyLoader(modules));
        dependencyManager.setDependencyLoaders(dependencyLoaders);

        WebStudio ws = mock(WebStudio.class);
        when(ws.getSystemConfigManager()).thenReturn(new ConfigurationManager(true, null));
        when(ws.getDependencyManager()).thenReturn(dependencyManager);

        pm = new ProjectModel(ws);
        moduleInfo = modules.get(0);
    }

    @Test
    public void testSetModule() throws Exception {
        // Initial field name
        setFieldName("area");
        pm.setModuleInfo(moduleInfo, ReloadType.SINGLE);
        Method methods[] = getExpenseInstanceClass(pm).getMethods();
        assertTrue(contains(methods, "setArea"));
        assertTrue(contains(methods, "getArea"));

        // Try to change field name and reload only current module. Getter/setter methods should change.
        setFieldName("dwarea");
        pm.reset(ReloadType.SINGLE);
        methods = getExpenseInstanceClass(pm).getMethods();
        assertTrue(contains(methods, "setDwarea"));
        assertTrue(contains(methods, "getDwarea"));
    }

    private boolean contains(Method methods[], String name) {
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name))
                return true;
        }
        return false;
    }

    private Class<?> getExpenseInstanceClass(ProjectModel pm) {
        return pm.getCompiledOpenClass().getTypes().get("org.openl.this::Expense").getInstanceClass();
    }

    private void setFieldName(String fieldName) throws FileNotFoundException, IOException {
        OutputStream os = null;
        try {
            Workbook wb = getWorkbook();
            Sheet testedSheet = wb.getSheetAt(0);
            Row row = testedSheet.getRow(testedSheet.getFirstRowNum() + 1);
            row.getCell(2).setCellValue(fieldName);

            os = new BufferedOutputStream(new FileOutputStream(RULES_FILE_NAME));
            wb.write(os);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private Workbook getWorkbook() throws IOException {
        if (pm.getCompiledOpenClass() != null) {
            XlsModuleSyntaxNode xlsModuleNode = ((XlsMetaInfo) pm.getCompiledOpenClass().getOpenClassWithErrors()
                    .getMetaInfo()).getXlsModuleNode();
            WorkbookSyntaxNode[] workbookNodes = xlsModuleNode.getWorkbookSyntaxNodes();
            return workbookNodes[0].getWorkbookSourceCodeModule().getWorkbook();
        }

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(RULES_FILE_NAME));
            return new HSSFWorkbook(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
