package org.openl.rules.excel.builder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import static org.openl.rules.excel.builder.export.AbstractOpenlTableExporter.DEFAULT_MARGIN;
import static org.openl.rules.excel.builder.export.AbstractOpenlTableExporter.TOP_LEFT_POSITION;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.VocabularyModel;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.DomainUtils;

class VocabularyTableExporterTest {

    private static final int TOP_MARGIN = TOP_LEFT_POSITION.getRow();
    private static final int LEFT_MARGIN = TOP_LEFT_POSITION.getColumn();

    @TempDir
    Path folder;

    @Test
    void writesTheVocabulariesAboveTheDatatypesThatUseThem() throws IOException {
        var bean = new DatatypeModel("Bean");
        bean.setFields(List.of(new FieldModel("kind", "Kind", "bla1"),
                new FieldModel("kinds", "Kind[]"),
                new FieldModel("level", "Level", 2)));
        var projectModel = new ProjectModel("vocabulary", false, asSet(bean), List.of(), List.of(), List.of());
        projectModel.setVocabularyModels(List.of(new VocabularyModel("Kind", "String", List.of("bla1", "bla2", "bla3")),
                new VocabularyModel("Level", "Integer", List.of("1", "2", "3"))));

        var file = folder.resolve("vocabulary.xlsx");
        try (var out = Files.newOutputStream(file)) {
            ExcelFileBuilder.generateDataTypes(projectModel, out);
        }

        try (InputStream in = Files.newInputStream(file); var wb = new XSSFWorkbook(in)) {
            var sheet = wb.getSheet("Datatypes");
            assertEquals("Datatype Kind <String>", text(sheet, TOP_MARGIN));
            assertEquals("bla1", text(sheet, TOP_MARGIN + 1));
            assertEquals("bla2", text(sheet, TOP_MARGIN + 2));
            assertEquals("bla3", text(sheet, TOP_MARGIN + 3));

            var level = TOP_MARGIN + 3 + DEFAULT_MARGIN;
            assertEquals("Datatype Level <Integer>", text(sheet, level));
            assertEquals(1, sheet.getRow(level + 1).getCell(LEFT_MARGIN).getNumericCellValue());
            assertEquals(3, sheet.getRow(level + 3).getCell(LEFT_MARGIN).getNumericCellValue());

            var beanHeader = level + 3 + DEFAULT_MARGIN;
            assertEquals("Datatype Bean", text(sheet, beanHeader));
            assertEquals("Kind[]", text(sheet, beanHeader + 2));
            // The default of a vocabulary field is written as the word or the number it is.
            assertEquals("bla1", sheet.getRow(beanHeader + 1).getCell(LEFT_MARGIN + 2).getStringCellValue());
            assertEquals(2, sheet.getRow(beanHeader + 3).getCell(LEFT_MARGIN + 2).getNumericCellValue());
            assertEquals(1, sheet.getNumMergedRegions(), "a vocabulary is a table of one column, without a merged header");
            assertEquals(beanHeader, sheet.getMergedRegion(0).getFirstRow());
        }

        var compiled = new RulesEngineFactory<>(file.toUri().toURL()).getCompiledOpenClass();
        assertFalse(compiled.hasErrors(), () -> compiled.getAllMessages().toString());
        var kind = assertInstanceOf(DomainOpenClass.class, compiled.getOpenClass().findType("Kind"));
        assertArrayEquals(new String[]{"bla1", "bla2", "bla3"}, DomainUtils.values(kind.getDomain()));
        var level = assertInstanceOf(DomainOpenClass.class, compiled.getOpenClass().findType("Level"));
        assertArrayEquals(new String[]{"1", "2", "3"}, DomainUtils.values(level.getDomain()));
        var beanType = compiled.getOpenClass().findType("Bean");
        assertEquals(kind, beanType.getField("kind").getType());
        assertEquals(kind, beanType.getField("kinds").getType().getComponentClass());
        var env = new SimpleRulesVM().getRuntimeEnv();
        var defaults = beanType.newInstance(env);
        assertEquals("bla1", beanType.getField("kind").get(defaults, env));
        assertEquals(2, beanType.getField("level").get(defaults, env));
    }

    private static String text(Sheet sheet, int row) {
        return sheet.getRow(row).getCell(LEFT_MARGIN).getStringCellValue();
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... args) {
        return new LinkedHashSet<>(List.of(args));
    }
}
