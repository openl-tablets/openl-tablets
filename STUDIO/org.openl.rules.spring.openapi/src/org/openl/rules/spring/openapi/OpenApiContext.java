package org.openl.rules.spring.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.openl.info.OpenLVersion;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class OpenApiContext {

    private final OpenAPI openAPI;
    private final Set<String> tagIds = new HashSet<>();
    private final Map<Class<?>, Map<String, Tag>> controllerTags = new HashMap<>();
    private final Map<Class<?>, ApiResponses> controllerResponses = new HashMap<>();

    public OpenApiContext() {
        this.openAPI = new OpenAPI()
                .info(new Info()
                        .version(OpenLVersion.getVersion())
                        .title("OpenL Tablets WebStudio API"))
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

    public void addAllTags(Collection<Tag> tags) {
        tags.forEach(this::addTagsItem);
    }

    public void addClassTags(Class<?> beanType, Collection<Tag> tagItems) {
        var tags = controllerTags.computeIfAbsent(beanType, key -> new HashMap<>());
        for (Tag tagItem : tagItems) {
            addTagsItem(tagItem);
            tags.putIfAbsent(tagItem.getName(), tagItem);
        }
    }

    public Map<String, Tag> getClassTags(Class<?> beanType) {
        return controllerTags.get(beanType);
    }

    public void addClassApiResponses(Class<?> beanType, ApiResponses responses) {
        controllerResponses.put(beanType, responses);
    }

    public ApiResponses getClassApiResponses(Class<?> beanType) {
        return controllerResponses.get(beanType);
    }
}
