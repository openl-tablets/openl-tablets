package org.openl.rules.serialization.jackson.org.openl.rules.context;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.openl.rules.enumeration.OriginsEnum;

public interface IRulesRuntimeContext {

    @JsonIgnore
    OriginsEnum getOrigin();

}
