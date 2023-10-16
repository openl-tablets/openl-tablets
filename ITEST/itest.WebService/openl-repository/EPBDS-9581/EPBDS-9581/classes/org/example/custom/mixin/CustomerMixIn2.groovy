package org.example.custom.mixin;

import java.util.Date;

import org.openl.rules.ruleservice.databinding.annotation.MixInClass;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@MixInClass(types = [org.openl.generated.beans.Customer2.class])
public abstract class CustomerMixIn2 {

    @JsonProperty(required = true)
    protected Integer customerID;

    @JsonIgnore
    protected Integer privateField;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH.mm")
    protected Date dob;

    @JsonProperty("gCd")
    @Schema(example = "male")
    protected String gender;
}

