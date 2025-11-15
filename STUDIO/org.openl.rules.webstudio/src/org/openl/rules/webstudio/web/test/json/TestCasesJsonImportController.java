package org.openl.rules.webstudio.web.test.json;

import java.io.InputStream;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * REST controller for importing test cases from JSON files.
 */
@RestController
@RequestMapping("/test/import")
@Tag(name = "Test Import", description = "APIs for importing test cases from JSON files")
public class TestCasesJsonImportController {

    private static final Logger LOG = LoggerFactory.getLogger(TestCasesJsonImportController.class);

    private final TestCasesJsonImportService importService;

    public TestCasesJsonImportController(TestCasesJsonImportService importService) {
        this.importService = importService;
    }

    /**
     * Import test cases from a JSON file.
     *
     * @param file The JSON file containing test cases
     * @return Response indicating success or failure
     */
    @Operation(summary = "Import test cases from JSON file", description = "Upload a JSON file containing test cases to add them to a test table")
    @PostMapping(value = "/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFromJson(
            @Parameter(description = "JSON file containing test cases", required = true) @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        if (!file.getOriginalFilename().endsWith(".json")) {
            return ResponseEntity.badRequest().body("Only JSON files are supported");
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the JSON file
            TestCasesImportRequest request = importService.parseJson(inputStream);

            LOG.info("Importing {} test cases for table '{}'",
                    request.getTestCases().size(),
                    request.getTableName());

            // Import the test cases
            String newTableUri = importService.importTestCases(request);

            // Mark the project as modified
            WebStudio studio = WebStudioUtils.getWebStudio();
            if (studio != null && studio.getCurrentProject() != null) {
                RulesProject project = studio.getCurrentProject();
                // The project will need to be saved by the user
                LOG.info("Test table created at URI: {}. Project needs to be saved.", newTableUri);
            }

            return ResponseEntity.ok()
                    .body(new ImportResponse(true,
                            "Successfully imported " + request.getTestCases()
                                    .size() + " test cases to table " + request.getTableName(),
                            newTableUri));

        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ImportResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            LOG.error("Failed to import test cases from JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImportResponse(false, "Failed to import: " + e.getMessage(), null));
        }
    }

    /**
     * Response object for import operation.
     */
    public static class ImportResponse {
        private final boolean success;
        private final String message;
        private final String tableUri;

        public ImportResponse(boolean success, String message, String tableUri) {
            this.success = success;
            this.message = message;
            this.tableUri = tableUri;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getTableUri() {
            return tableUri;
        }
    }
}
