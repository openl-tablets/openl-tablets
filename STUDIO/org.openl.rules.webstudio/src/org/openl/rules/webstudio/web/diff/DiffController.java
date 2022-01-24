package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import org.openl.util.FileTool;
import org.openl.util.FileUtils;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/public/compare")
public class DiffController {
    private static final Logger LOG = LoggerFactory.getLogger(DiffController.class);

    private final DiffManager diffManager;

    public DiffController(DiffManager diffManager) {
        this.diffManager = diffManager;
    }

    @PostMapping(value = "xls", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> compareXls(@RequestParam(value = "file1", required = false) MultipartFile file1,
            @RequestParam(value = "file2", required = false) MultipartFile file2,
            @RequestParam(value = "fileName", required = false) String fileName) {
        try {
            String requestId = UUID.randomUUID().toString();

            File excelFile1 = createTempFile(file1.getInputStream(), "file1");
            File excelFile2 = createTempFile(file2.getInputStream(), "file2");
            diffManager.add(requestId, new ShowDiffController(excelFile1, excelFile2, fileName));

            String root = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            if (root.endsWith("/web") || root.endsWith("/rest")) {
                // Remove prefix for rest service because we return a link to a html page.
                root = root.substring(0, root.lastIndexOf('/'));
            }
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(new URI(root + "/faces/pages/public/showDiff.xhtml?id=" + requestId))
                .build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private File createTempFile(InputStream inputStream, String fullName) throws FileNotFoundException {
        if (inputStream == null) {
            return null;
        }
        File tempFile = FileTool.toTempFile(inputStream, FileUtils.getName(fullName));
        if (tempFile == null) {
            throw new FileNotFoundException(String.format("Cannot create temp file for '%s'", fullName));
        }
        if (tempFile.length() == 0) {
            FileUtils.deleteQuietly(tempFile);
            return null;
        }
        return tempFile;
    }

}
