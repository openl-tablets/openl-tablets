package mixin

import com.fasterxml.jackson.annotation.JsonProperty
import org.openl.rules.ruleservice.databinding.annotation.MixInClass

@MixInClass("org.openl.generated.beans.Policy")
abstract class PolicyMixin {

    @JsonProperty(required = true)
    abstract String getPolicyNumber();

    @JsonProperty(required = true)
    abstract Integer getRevisionNo();

}
