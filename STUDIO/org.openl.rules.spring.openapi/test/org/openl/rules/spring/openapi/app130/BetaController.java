package org.openl.rules.spring.openapi.app130;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BetaController {

    @GetMapping("/beta")
    public ResponseEntity<Void> status() {
        return null;
    }
}
