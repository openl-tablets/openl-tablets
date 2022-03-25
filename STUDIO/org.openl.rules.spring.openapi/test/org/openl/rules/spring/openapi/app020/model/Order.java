package org.openl.rules.spring.openapi.app020.model;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class Order {
    @JsonProperty("complete")
    private Boolean complete;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("petId")
    private Long petId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("shipDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime shipDate;

    /**
     * Order Status
     */
    public enum StatusEnum {
        PLACED("placed"),

        APPROVED("approved"),

        DELIVERED("delivered");

        private final String value;

        StatusEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

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

    @JsonProperty("status")
    @Schema(description = "Order Status")
    private StatusEnum status;

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPetId() {
        return petId;
    }

    public void setPetId(Long petId) {
        this.petId = petId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public OffsetDateTime getShipDate() {
        return shipDate;
    }

    public void setShipDate(OffsetDateTime shipDate) {
        this.shipDate = shipDate;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }
}
