package org.openl.rules.spring.openapi.app060;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping(value = "/sayHello.json")
    public Map<Object, Object> getHttpInfo(HttpServletRequest request, @Parameter(hidden = true) @RequestHeader HttpHeaders headers) {
        return null;
    }

}
