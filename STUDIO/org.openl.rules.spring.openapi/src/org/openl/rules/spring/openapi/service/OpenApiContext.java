package org.openl.rules.spring.openapi.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.info.OpenLVersion;
import org.openl.rules.spring.openapi.OpenApiUtils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI Context
 */
final class OpenApiContext {

    private final OpenAPI openAPI;
    private final Set<String> tagIds = new HashSet<>();
    private final Map<Class<?>, Map<String, Tag>> controllerTags = new HashMap<>();

    public OpenApiContext() {
        this.openAPI = new OpenAPI()
            .info(new Info().version(OpenLVersion.getVersion()).title("OpenL Tablets WebStudio API"))
            .addServersItem(new Server().url(OpenApiUtils.getRequestBasePath()))
            .paths(new Paths())
            .components(new Components());
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    public Paths getPaths() {
        return openAPI.getPaths();
    }

    public Components getComponents() {
        return openAPI.getComponents();
    }

    public void addTagsItem(Tag tagItem) {
        if (!tagIds.contains(tagItem.getName())) {
            tagIds.add(tagItem.getName());
            openAPI.addTagsItem(tagItem);
        }
    }

    /**
     * Associate tags with class
     *
     * @param beanType bean class
     * @param tagItems the list of tags of this bean
     */
    public void addClassTags(Class<?> beanType, Collection<Tag> tagItems) {
        var tags = controllerTags.computeIfAbsent(beanType, key -> new HashMap<>());
        for (Tag tagItem : tagItems) {
            addTagsItem(tagItem);
            tags.putIfAbsent(tagItem.getName(), tagItem);
        }
    }

    /**
     * Get the list of tags for bean class
     * 
     * @param beanType bean class
     * @return the list of tags or null
     */
    public Map<String, Tag> getClassTags(Class<?> beanType) {
        return controllerTags.get(beanType);
    }

}
