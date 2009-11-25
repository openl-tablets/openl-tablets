package org.openl.rules.liveexcel.usermodel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openl.rules.liveexcel.ServiceModelAPI;

import com.exigen.le.calc.PropertyEvaluator;

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

    public Set<String> getRootNames() {
        return new HashSet<String>();
    }

    public Class<?> getRootType(String rootName) {
        return Object.class;
    }

}
