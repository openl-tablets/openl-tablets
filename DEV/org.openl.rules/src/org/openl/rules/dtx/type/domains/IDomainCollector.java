package org.openl.rules.dtx.type.domains;

import java.util.Map;

public interface IDomainCollector {
    
    void gatherDomains(Map<String, Object> methodProperties);
    
    IDomainAdaptor getGatheredDomain();
    
}
