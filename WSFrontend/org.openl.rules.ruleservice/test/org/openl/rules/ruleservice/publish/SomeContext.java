package org.openl.rules.ruleservice.publish;

import lombok.Getter;
import lombok.Setter;

import org.openl.rules.annotations.ContextProperty;

public class SomeContext {

    @Getter(onMethod_ = {@ContextProperty("lob")})
    @Setter
    private String lob;
}
