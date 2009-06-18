package org.openl.rules.liveexcel.usermodel;

import java.util.Collections;
import java.util.Set;

import org.openl.rules.liveexcel.ServiceModelAPI;

public class EmptyServiceModelAPI extends ServiceModelAPI {

    public EmptyServiceModelAPI() {
        super(null);
    }

    @Override
    public Set<String> getAllServiceModelUDFs() {
        return Collections.emptySet();
    }

    @Override
    public Class<?> getServiceModelObjectDomainType(String name) {
        return Object.class;
    }

    @Override
    public Object getValue(String name, Object object) {
        return null;
    }
    
    

}
