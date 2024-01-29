package org.openl.rules.rest.dependency;

import java.util.HashSet;
import java.util.Set;

import io.swagger.v3.oas.annotations.Parameter;

public class Table {

    @Parameter(description = "project.table.name.desc", required = true)
    private String name;
    @Parameter(description = "project.table.id.desc", required = true)
    private String id;
    @Parameter(description = "project.table.url.desc", required = true)
    private String url;

    @Parameter(description = "project.table.dependencies.desc")
    private Set<String> dependencies = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return getName();
    }

}
