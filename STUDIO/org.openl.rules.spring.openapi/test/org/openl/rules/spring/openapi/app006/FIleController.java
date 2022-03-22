package org.openl.rules.spring.openapi.app006;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FIleController {

    @PostMapping(value = "/upload/bulk", consumes = "multipart/form-data")
    ResponseEntity<Void> bulkUpload(@RequestPart("files") List<MultipartFile> multipartFiles) {
        return null;
    }

    @PostMapping(value ="/upload", consumes = "multipart/form-data")
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file) {
        return null;
    }

    @PostMapping(value ="/form/{name}", consumes = "multipart/form-data")
    public ResponseEntity<Void> form(@PathVariable("name") final String name,
                                     @Parameter(name = "configuration") @RequestPart(value = "configuration") final String configuration,
                                     @RequestPart(value = "file") final MultipartFile file) {
        return null;
    }

}
