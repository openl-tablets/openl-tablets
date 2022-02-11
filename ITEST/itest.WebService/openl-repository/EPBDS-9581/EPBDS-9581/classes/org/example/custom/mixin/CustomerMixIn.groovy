package org.example.custom.mixin;

import java.util.Date;

import org.openl.rules.ruleservice.databinding.annotation.MixInClass;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@MixInClass("org.openl.generated.beans.Customer")
public abstract class CustomerMixIn {

    @JsonProperty(required = true)
    protected Integer customerID;

    @JsonIgnore
    protected Integer privateField;

    @JsonFormat(pattern = "yyyy-MM-dd")
    protected Date dob;

    @JsonProperty("genderCd")
    @Schema(example = "male")
    protected String gender;
}

