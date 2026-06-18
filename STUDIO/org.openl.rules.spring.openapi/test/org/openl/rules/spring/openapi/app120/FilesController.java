package org.openl.rules.spring.openapi.app120;

import java.io.InputStream;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Several POST handlers mapped to the same path and HTTP method, differing only by the consumed media type
 * (multipart, raw and archive). OpenAPI collapses them into a single operation, so their shared {@code folder} and
 * {@code path} path parameters and {@code createFolders} query parameter must appear only once. The archive variant
 * declares a different {@code createFolders} default, so the merged operation must not advertise any single default.
 */
@RestController
@RequestMapping("/files/{folder}")
public class FilesController {

    @PostMapping(value = "/{*path}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> create(
            @PathVariable("folder") String folder,
            @PathVariable @Parameter(description = "File path") String path,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "createFolders", defaultValue = "false") boolean createFolders) {
        return null;
    }

    @PostMapping(value = "/{*path}")
    public ResponseEntity<Void> createRaw(
            @PathVariable("folder") String folder,
            @PathVariable @Parameter(description = "File path") String path,
            @RequestParam(value = "createFolders", defaultValue = "false") boolean createFolders,
            InputStream content) {
        return null;
    }

    @PostMapping(value = "/{*path}", consumes = "application/zip")
    public ResponseEntity<Void> uploadArchive(
            @PathVariable("folder") String folder,
            @PathVariable @Parameter(description = "File path") String path,
            @RequestParam(value = "createFolders", defaultValue = "true") boolean createFolders,
            InputStream content) {
        return null;
    }
}
