package org.openl.rules.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.spring.env.DynamicPropertySource;
import org.openl.spring.env.PropertyBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/property")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// TODO Think about using of {@link org.openl.config.PropertiesHolder} and {@link org.openl.config.InMemoryProperties}
// TODO instead of custom one {@link PropertyBean}
// TODO Refactor this API
public class PropertyService {

    @Autowired
    private PropertyBean propertyBean;

    @GET
    @Path("/current")
    public Map<String, String> getProperties() {
        DynamicPropertySource dynamicPropertySource = DynamicPropertySource.get();
        Map<String, String> currentPropertyMap = new HashMap<>(propertyBean.getPropertyMap());
        currentPropertyMap.putAll(new HashMap<>((Map) dynamicPropertySource.getProperties()));
        return currentPropertyMap;
    }

    @GET
    @Path("/webStudio")
    public Properties getWebStudioProperties() {
        DynamicPropertySource dynamicPropertySource = DynamicPropertySource.get();
        return dynamicPropertySource.getProperties();
    }

    @GET
    @Path("/default")
    public Map<String, String> getDefaultProperties() {
        return propertyBean.getDefaultPropertyMap();
    }

    @POST
    @Path("/save")
    public void saveProperties(Map<String, String> properties) throws IOException {
        DynamicPropertySource.get().save(properties);
    }
}
