package org.openl.rules.spring.openapi.app070;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Lookup("lookupMock")
    public String getLookupMock() {
        throw new IllegalStateException();
    }

    @RequestMapping({"/sayHello.json", "/{name}/sayHello"})
    public Map<Object, Object> getHttpInfo(@PathVariable("name") Optional<String> name) {
        return null;
    }

}
