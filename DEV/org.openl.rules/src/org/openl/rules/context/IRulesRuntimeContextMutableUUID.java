package org.openl.rules.context;

import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface IRulesRuntimeContextMutableUUID {
    @XmlTransient
    UUID contextUUID();
}
