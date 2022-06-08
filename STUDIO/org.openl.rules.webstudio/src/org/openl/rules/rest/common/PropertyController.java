package org.openl.rules.rest.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.swagger.v3.oas.annotations.Operation;
import org.openl.rules.rest.SecurityChecker;
import org.openl.rules.security.Privileges;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertyBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

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
    public Map<String, String> getProperties() {
        SecurityChecker.allow(Privileges.ADMIN);
        DynamicPropertySource dynamicPropertySource = DynamicPropertySource.get();
        Map<String, String> currentPropertyMap = new HashMap<>(propertyBean.getPropertyMap());
        currentPropertyMap.putAll(new HashMap(dynamicPropertySource.getProperties()));
        return currentPropertyMap;
    }

    @GetMapping("/webStudio")
    @Hidden
    public Properties getWebStudioProperties() {
        SecurityChecker.allow(Privileges.ADMIN);
        DynamicPropertySource dynamicPropertySource = DynamicPropertySource.get();
        return dynamicPropertySource.getProperties();
    }

    @GetMapping("/default")
    @Hidden
    public Map<String, String> getDefaultProperties() {
        return propertyBean.getDefaultPropertyMap();
    }

    @Operation(summary = "config.download-app-props.summary", description = "config.download-app-props.desc")
    @GetMapping(value = "/application.properties", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ClassPathResource> getApplicationProperties() throws IOException {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(new ClassPathResource("application-example.properties"));
    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Hidden
    public void saveProperties(@RequestBody Map<String, String> properties) throws IOException {
        SecurityChecker.allow(Privileges.ADMIN);
        DynamicPropertySource.get().save(properties);
    }
}
