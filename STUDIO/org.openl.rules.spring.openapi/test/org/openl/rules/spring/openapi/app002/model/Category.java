package org.openl.rules.spring.openapi.app002.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Category
 */
public class Category {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
