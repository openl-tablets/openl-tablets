package org.openl.rules.spring.openapi.app070;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

@RestController
public abstract class HelloController {

    @Lookup("lookupMock")
    public abstract String getLookupMock();

    @RequestMapping({"/sayHello.json", "/{name}/sayHello"})
    public Map<Object, Object> getHttpInfo(@PathVariable("name") Optional<String> name) {
        return null;
    }

}
