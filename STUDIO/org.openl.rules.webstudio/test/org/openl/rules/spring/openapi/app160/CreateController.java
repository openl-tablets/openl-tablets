package org.openl.rules.spring.openapi.app160;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * A multipart create handler whose form parameters carry metadata the generator must honor. The
 * {@code status} parameter's {@code @Schema} allowable values differ from its Java enum constants (as a
 * converted wire code would), and the other parameters declare binding defaults. The generated form-data
 * schema must show the allowable values as sent and the declared defaults — not the enum constants, and
 * not a missing default.
 */
@RestController
public class CreateController {

    public enum Status {
        LOCAL,
        VIEWING,
        CLOSED
    }

    @PostMapping(value = "/projects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> create(
            @RequestParam("template") MultipartFile file,
            @Parameter(description = "Status after create", schema = @Schema(allowableValues = {"OPENED", "CLOSED"}))
            @RequestParam(value = "status", required = false) Status status,
            @Parameter(description = "Models module name")
            @RequestParam(value = "modelsModuleName", required = false, defaultValue = "Models") String modelsModuleName,
            @Parameter(description = "Overwrite existing")
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite) {
        return null;
    }
}
