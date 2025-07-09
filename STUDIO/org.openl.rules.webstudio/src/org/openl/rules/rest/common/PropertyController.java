package org.openl.rules.rest.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.security.AdminPrivilege;
import org.openl.spring.env.DefaultPropertySource;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertyBean;

@RestController
@RequestMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
// TODO Think about using of {@link org.openl.config.PropertiesHolder} and {@link org.openl.config.InMemoryProperties}
// TODO instead of custom one {@link PropertyBean}
// TODO Refactor this API
public class PropertyController {

    private final PropertyBean propertyBean;

    @Autowired
    public PropertyController(PropertyBean propertyBean) {
        this.propertyBean = propertyBean;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/current")
    @Hidden
    @AdminPrivilege
    @Deprecated(forRemoval = true)
    public Map<String, String> getProperties() {
        DynamicPropertySource dynamicPropertySource = DynamicPropertySource.get();
        Map<String, String> currentPropertyMap = new HashMap<>(propertyBean.getPropertyMap());
        currentPropertyMap.putAll(dynamicPropertySource.getProperties());
        return currentPropertyMap;
    }

    @GetMapping("/webStudio")
    @Hidden
    @AdminPrivilege
    @Deprecated(forRemoval = true)
    public Map<String, String> getWebStudioProperties() {
        DynamicPropertySource dynamicPropertySource = DynamicPropertySource.get();
        return dynamicPropertySource.getProperties();
    }

    @GetMapping("/default")
    @Hidden
    @Deprecated(forRemoval = true)
    public Map<String, String> getDefaultProperties() {
        return propertyBean.getDefaultPropertyMap();
    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AdminPrivilege
    @Hidden
    @Deprecated(forRemoval = true)
    public void saveProperties(@RequestBody Map<String, String> properties) throws IOException {
        DynamicPropertySource.get().save(properties);
    }
}
