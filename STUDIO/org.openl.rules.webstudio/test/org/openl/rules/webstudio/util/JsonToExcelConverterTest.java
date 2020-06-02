package org.openl.rules.webstudio.util;

import java.io.IOException;

import org.junit.Test;
import org.openl.rules.webstudio.util.converter.OpenAPIModelConverter;
import org.openl.rules.webstudio.util.converter.impl.JsonToExcelConverter;

public class JsonToExcelConverterTest {

    private static String JSON_FILE_NAME = "test.converter/Example3-AutoPolicyCalculationOpenAPI.json";

    @Test
    public void testAutoPolicyJson() throws IOException {
        OpenAPIModelConverter openAPIModelConverter = new JsonToExcelConverter();
        openAPIModelConverter.extractDataTypes(JSON_FILE_NAME);
    }
}
