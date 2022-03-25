package org.openl.rules.spring.openapi.app011;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/greetings")
public class HelloController {

    @GetMapping(value = "/hello")
    @Deprecated
    ResponseEntity<Void> sayHello() {
        return null;
    }

    @PostMapping("/sayHello")
    public void sayHello2(@RequestBody String name) {

    }

}
