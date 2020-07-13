package org.openl.rules;

import java.io.IOException;

import org.junit.Test;
import org.openl.rules.excel.builder.ExcelFileBuilder;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIExperiments;
import org.openl.rules.openapi.impl.OpenAPIToExcelModelConverter;

public class OpenAPIToExcelConverterTest {

    private static String JSON_FILE_NAME = "test.converter/Example3-AutoPolicyCalculationOpenAPI.json";
    private static String BANK_RATING = "test.converter/BankRating.json";

    private static String EXTERNAL_LINKS_JSON_TEST = "test.converter/external_links/Driver.json";

    @Test
    public void testAutoPolicyJson() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel(JSON_FILE_NAME);
    }

    @Test
    public void testBankRating() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel(BANK_RATING);
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testFolderWithJsonFiles() throws IOException {
        // OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel(EXTERNAL_LINKS_JSON_TEST);
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testNested() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/multiple_objects_inside/Continent.json");
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
        // OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/inline_object/InlineSample.json");
    }

    @Test
    public void testInlin1e() throws IOException {
        // OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/inline_object/inline.yaml");
    }

    @Test
    public void testInlineWithBodyNamedObject() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/inline_object/InlineSample_bodyNamedObjectInside.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testCombined() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/inheritance_and_polymorphism/anyOf.yml");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testCombinedRequest() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/inheritance_and_polymorphism/oneOfRequest.yml");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testAllOf() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/inheritance_and_polymorphism/allOf.yml");
    }

    @Test
    public void testReusableBody() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/reusable_body/reusablebody.yml");
    }

    @Test
    public void testRequests() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIToExcelModelConverter();
        ProjectModel projectModel = converter.extractProjectModel("Requests.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testAllOfInsideOneOf() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter
            .extractProjectModel("test.converter/inheritance_and_polymorphism/allOfInsideOneOf.yml");
    }

    @Test
    public void testSimpleDatatype() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/datatype/datatype_simple.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }

    @Test
    public void testNesting() throws IOException {
        OpenAPIModelConverter converter = new OpenAPIExperiments();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/datatype/datatype_with_parent.json");
        ExcelFileBuilder.generateExcelFile(projectModel);
    }
}
