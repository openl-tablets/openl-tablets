package org.openl.rules;

import java.io.IOException;

import org.junit.Test;
import org.openl.rules.model.scaffolding.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIToExcelModelConverter;

public class OpenAPIToExcelConverterTest {

    private static String JSON_FILE_NAME = "test.converter/Example3-AutoPolicyCalculationOpenAPI.json";
    private static String BANK_RATING = "test.converter/BankRating.json";

    private static String EXTERNAL_LINKS_JSON_TEST = "test.converter/external_links/Driver.json";

    @Test
    public void testAutoPolicyJson() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel(JSON_FILE_NAME);
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testBankRating() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel(BANK_RATING);
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testFolderWithJsonFiles() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel(EXTERNAL_LINKS_JSON_TEST);
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testNested() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/multiple_objects_inside/Continent.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testYaml() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/openapi.yaml");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testInline() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/inline_object/InlineSample.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testInlineWithBodyNamedObject() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/inline_object/InlineSample_bodyNamedObjectInside.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testCombined() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/combinedSchemas/combined.yml");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testCombinedRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/combinedSchemas/combined_request.yml");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }
}
