package org.openl.rules.spring.openapi.app020.model;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class Pet {

    @Getter
    @JsonProperty("tags")
    @Setter
    private List<Tag> tags;

    @Getter
    @JsonProperty("category")
    @Setter
    private Category category;

    @Getter
    @JsonProperty("id")
    @Setter
    private Long id;

    @Getter
    @JsonProperty("name")
    @Parameter(example = "doggie")
    @Setter
    @NotNull
    private String name;

    @Getter
    @JsonProperty("photoUrls")
    @NotNull
    @Setter
    private List<String> photoUrls = new ArrayList<>();

    /**
     * pet status in the store
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public enum StatusEnum {
        AVAILABLE("available"),

        PENDING("pending"),

        SOLD("sold");

        @Getter(onMethod_ = {@JsonValue})
        private final String value;

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StatusEnum fromValue(String value) {
            for (StatusEnum b : StatusEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @Getter
    @JsonProperty("status")
    @Parameter(description = "pet status in the store")
    @Setter
    private StatusEnum status;
}
