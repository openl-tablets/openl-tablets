package org.openl.rules.spring.openapi.app020.model;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

public class Order {
    @Getter
    @JsonProperty("complete")
    @Setter
    private Boolean complete;

    @Getter
    @JsonProperty("id")
    @Setter
    private Long id;

    @Getter
    @JsonProperty("petId")
    @Setter
    private Long petId;

    @Getter
    @JsonProperty("quantity")
    @Setter
    private Integer quantity;

    @JsonProperty("shipDate")
    @Setter
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Getter
    private OffsetDateTime shipDate;

    /**
     * Order Status
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public enum StatusEnum {
        PLACED("placed"),

        APPROVED("approved"),

        DELIVERED("delivered");

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
    @Schema(description = "Order Status")
    @Setter
    private StatusEnum status;
}
