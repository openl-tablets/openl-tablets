package org.openl.rules.testmethod.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.data.PrimaryKeyField;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class AbstractParameterExportTest {

    protected SXSSFWorkbook workbook;
    protected SXSSFSheet sheet;

    protected void assertRowEquals(Row row, String... values) {
        assertNotNull(row, "Row is absent. Expected values: " + Arrays.toString(values));

        int colNum = BaseExport.FIRST_COLUMN;
        for (String value : values) {
            Cell cell = row.getCell(colNum);
            assertNotNull(cell, "Column " + colNum + " is absent");
            assertEquals(value, cell.getStringCellValue(), "Incorrect column " + colNum);
            colNum++;
        }

        short lastCellNum = row.getLastCellNum();
        int total = lastCellNum - BaseExport.FIRST_COLUMN;
        if (values.length < total) {
            StringBuilder sb = new StringBuilder("Missed values: ");
            int count = 0;
            while (colNum < lastCellNum) {
                if (count > 0) {
                    sb.append(',');
                }
                sb.append(row.getCell(colNum++).getStringCellValue());
                count++;
            }

            fail(sb.toString());
        }
    }

    protected ParameterWithValueDeclaration[] params(Object... values) {
        return params(null, null, values);
    }

    protected ParameterWithValueDeclaration[] params(Class[] types, Object... values) {
        return params(null, types, values);
    }

    protected ParameterWithValueDeclaration[] params(String[] pkValues, Object... values) {
        return params(pkValues, null, values);
    }

    protected ParameterWithValueDeclaration[] params(String[] pkValues, Class[] types, Object... values) {
        ParameterWithValueDeclaration[] params = new ParameterWithValueDeclaration[values.length];
        for (int i = 0; i < values.length; i++) {

            IOpenClass type;
            if (types == null) {
                type = ParameterWithValueDeclaration.getParamType(values[i]);
            } else {
                type = JavaOpenClass.getOpenClass(types[i]);
            }

            PrimaryKeyField field = mockKeyField(pkValues, i);
            params[i] = new ParameterWithValueDeclaration("p" + (i + 1), values[i], type, field);
        }
        return params;
    }

    protected PrimaryKeyField mockKeyField(String[] pkValues, int i) {
        if (pkValues != null && pkValues[i] != null) {
            PrimaryKeyField field = mock(PrimaryKeyField.class);
            when(field.get(any(), any())).thenReturn(pkValues[i]);
            return field;
        }

        return null;
    }

    protected XSSFSheet saveAndReadSheet() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        workbook.close();
        return new XSSFWorkbook(new ByteArrayInputStream(stream.toByteArray())).getSheetAt(0);
    }

    protected List<TestUnitsResults> mockResults(ParameterWithValueDeclaration[]... paramsForEachCase) {
        return Collections.singletonList(mockResult("TestRule", paramsForEachCase));
    }

    protected TestUnitsResults mockResult(String testMethodName, ParameterWithValueDeclaration[]... paramsForEachCase) {
        IOpenMethod testedMethod = mock(IOpenMethod.class);
        when(testedMethod.getName()).thenReturn(testMethodName);

        List<TestDescription> results = new ArrayList<>();
        for (int i = 0; i < paramsForEachCase.length; i++) {
            TestDescription testDescription = mock(TestDescription.class);
            when(testDescription.getId()).thenReturn("#" + (i + 1));
            when(testDescription.getExecutionParams()).thenReturn(paramsForEachCase[i]);
            when(testDescription.getTestedMethod()).thenReturn(testedMethod);
            results.add(testDescription);
        }

        TestSuite testSuite = new TestSuite(results.toArray(new TestDescription[0]));
        return new TestUnitsResults(testSuite);
    }

    public static class ComplexObj {
        public List<Object> paramList;
        public Map<String, Integer> mapValues;

        public ComplexObj(List<Object> paramList, Map<String, Integer> mapValues) {
            this.paramList = paramList;
            this.mapValues = mapValues;
        }
    }

}
