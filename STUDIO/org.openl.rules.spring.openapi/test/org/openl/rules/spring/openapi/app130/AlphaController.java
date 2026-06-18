package org.openl.rules.spring.openapi.app130;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Together with {@link BetaController} this exposes two handlers named {@code status}, so the generated operation ids
 * collide and one of them gets a numeric suffix. The unsuffixed id goes to the handler whose method signature sorts
 * first, which the reader keeps stable by processing handlers in a deterministic order.
 */
@RestController
public class AlphaController {

    @GetMapping("/alpha")
    public ResponseEntity<Void> status() {
        return null;
    }
}
