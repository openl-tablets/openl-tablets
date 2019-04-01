package org.openl.rules.table.xls.builder;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Test;
import org.openl.rules.dt.DecisionTableHelper;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.DataTableUserDefinedTypeField.PredefinedTypeChecker;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class DataTableBuilderTest {
    private static final String CLASS_NAME = "Policy";
    private static final String TABLE_NAME = "testPolicy";
    private static final String FILE_NAME = "MyHello.xls";
    private static final String TEST_FOLDER = "target/unit_tests";
    private static final String DATA_TABLE_POLICY = "Data " + CLASS_NAME + " " + TABLE_NAME;

    @Test
    public void testDataWithForeignKey() throws Exception {
        String fileName = String.format("%s/test1%s", TEST_FOLDER, FILE_NAME);
        XlsSheetGridModel openlSheet = DecisionTableHelper.createVirtualGrid();

        DataTableBuilder builder = new DataTableBuilder(openlSheet);
        builder.beginTable(5, 4);

        builder.writeHeader(CLASS_NAME, TABLE_NAME);
        // builder.writeProperties(properties, null);

        List<DataTableField> fields = new ArrayList<>();
        DataTableField field;

        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.STRING, "name", "Name", predefinedChecker));
        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.STRING, "address", "Address", predefinedChecker));
        field = new DataTableUserDefinedTypeField(JavaOpenClass.getOpenClass(Car.class),
            "newCars",
            "New Cars",
            predefinedChecker);
        field.setForeignKeyTable("testCars");
        fields.add(field);
        field = new DataTableUserDefinedTypeField(JavaOpenClass.getOpenClass(Car.class),
            "oldCars",
            "Old Cars",
            predefinedChecker);
        field.setForeignKeyTable("testCars");
        field.setForeignKeyColumn("name");
        fields.add(field);
        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.getOpenClass(IntRange.class),
            "costRange",
            "Costs",
            predefinedChecker));

        builder.writeFieldNames(fields);

        IGridRegion region = builder.getTableRegion();

        builder.endTable();

        new File(TEST_FOLDER).mkdirs();

        try {
            openlSheet.getSheetSource().getWorkbookSource().saveAs(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            fail(String.format("Cannot save file : %s", fileName));
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            fail(String.format("Cannot create input stream for file : %s", fileName));
        }
        HSSFWorkbook tested = null;
        try {
            tested = new HSSFWorkbook(fis);
        } catch (IOException e) {
            e.printStackTrace();
            fail(String.format("Cannot open file with poi: %s", fileName));
        }

        assertNotNull(tested);
        Sheet testedSheet = tested.getSheetAt(0);
        Row firstRow = testedSheet.getRow(testedSheet.getFirstRowNum());
        assertEquals(DATA_TABLE_POLICY, firstRow.getCell(region.getLeft()).getStringCellValue());

        Row secondRow = testedSheet.getRow(testedSheet.getFirstRowNum() + 1);
        assertEquals("name", secondRow.getCell(region.getLeft()).getStringCellValue());
        assertEquals("address", secondRow.getCell(region.getLeft() + 1).getStringCellValue());
        assertEquals("newCars", secondRow.getCell(region.getLeft() + 2).getStringCellValue());
        assertEquals("oldCars", secondRow.getCell(region.getLeft() + 3).getStringCellValue());
        assertEquals("costRange", secondRow.getCell(region.getLeft() + 4).getStringCellValue());

        Row thirdRow = testedSheet.getRow(testedSheet.getFirstRowNum() + 2);
        assertEquals("", thirdRow.getCell(region.getLeft()).getStringCellValue());
        assertEquals("", thirdRow.getCell(region.getLeft() + 1).getStringCellValue());
        assertEquals(">testCars", thirdRow.getCell(region.getLeft() + 2).getStringCellValue());
        assertEquals(">testCars name", thirdRow.getCell(region.getLeft() + 3).getStringCellValue());
        assertEquals("", thirdRow.getCell(region.getLeft() + 4).getStringCellValue());

        Row fourthRow = testedSheet.getRow(testedSheet.getFirstRowNum() + 3);
        assertEquals("Name", fourthRow.getCell(region.getLeft()).getStringCellValue());
        assertEquals("Address", fourthRow.getCell(region.getLeft() + 1).getStringCellValue());
        assertEquals("New Cars", fourthRow.getCell(region.getLeft() + 2).getStringCellValue());
        assertEquals("Old Cars", fourthRow.getCell(region.getLeft() + 3).getStringCellValue());
        assertEquals("Costs", fourthRow.getCell(region.getLeft() + 4).getStringCellValue());
    }

    @Test
    public void testDataWithoutForeignKey() throws Exception {
        String fileName = String.format("%s/test1%s", TEST_FOLDER, FILE_NAME);
        XlsSheetGridModel openlSheet = DecisionTableHelper.createVirtualGrid();

        DataTableBuilder builder = new DataTableBuilder(openlSheet);
        builder.beginTable(5, 4);

        builder.writeHeader(CLASS_NAME, TABLE_NAME);
        // builder.writeProperties(properties, null);

        List<DataTableField> fields = new ArrayList<>();

        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.STRING, "name", "Name", predefinedChecker));
        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.STRING, "address", "Address", predefinedChecker));
        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.getOpenClass(Car.class),
            "newCars",
            "New Cars",
            predefinedChecker));
        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.getOpenClass(Car.class),
            "oldCars",
            "Old Cars",
            predefinedChecker));
        fields.add(new DataTableUserDefinedTypeField(JavaOpenClass.getOpenClass(IntRange.class),
            "costRange",
            "Costs",
            predefinedChecker));

        builder.writeFieldNames(fields);

        IGridRegion region = builder.getTableRegion();

        builder.endTable();

        new File(TEST_FOLDER).mkdirs();

        try {
            openlSheet.getSheetSource().getWorkbookSource().saveAs(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            fail(String.format("Cannot save file : %s", fileName));
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            fail(String.format("Cannot create input stream for file : %s", fileName));
        }
        HSSFWorkbook tested = null;
        try {
            tested = new HSSFWorkbook(fis);
        } catch (IOException e) {
            e.printStackTrace();
            fail(String.format("Cannot open file with poi: %s", fileName));
        }

        assertNotNull(tested);
        Sheet testedSheet = tested.getSheetAt(0);
        Row firstRow = testedSheet.getRow(testedSheet.getFirstRowNum());
        assertEquals(DATA_TABLE_POLICY, firstRow.getCell(region.getLeft()).getStringCellValue());

        Row secondRow = testedSheet.getRow(testedSheet.getFirstRowNum() + 1);
        assertEquals("name", secondRow.getCell(region.getLeft()).getStringCellValue());
        assertEquals("address", secondRow.getCell(region.getLeft() + 1).getStringCellValue());
        assertEquals("newCars", secondRow.getCell(region.getLeft() + 2).getStringCellValue());
        assertEquals("oldCars", secondRow.getCell(region.getLeft() + 3).getStringCellValue());
        assertEquals("costRange", secondRow.getCell(region.getLeft() + 4).getStringCellValue());

        Row fourthRow = testedSheet.getRow(testedSheet.getFirstRowNum() + 2);
        assertEquals("Name", fourthRow.getCell(region.getLeft()).getStringCellValue());
        assertEquals("Address", fourthRow.getCell(region.getLeft() + 1).getStringCellValue());
        assertEquals("New Cars", fourthRow.getCell(region.getLeft() + 2).getStringCellValue());
        assertEquals("Old Cars", fourthRow.getCell(region.getLeft() + 3).getStringCellValue());
        assertEquals("Costs", fourthRow.getCell(region.getLeft() + 4).getStringCellValue());

    }

    private PredefinedTypeChecker predefinedChecker = new PredefinedTypeChecker() {

        @Override
        public boolean isPredefined(IOpenClass type) {
            Class<?> instanceClass = type.getInstanceClass();

            if (IntRange.class.equals(instanceClass)) {
                return true;
            }

            return false;
        }
    };

    @SuppressWarnings("unused")
    private static class Car {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
