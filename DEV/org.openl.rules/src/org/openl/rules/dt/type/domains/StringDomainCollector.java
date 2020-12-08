package org.openl.rules.dt.type.domains;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.domain.EnumDomain;
import org.openl.util.StringUtils;

public class StringDomainCollector implements IDomainCollector {

    private String propertyToSearch;

    private Set<String> stringProp = new HashSet<>();

    public StringDomainCollector(String propertyToSearch) {
        this.propertyToSearch = propertyToSearch;
    }

    @Override
    public void gatherDomains(Map<String, Object> methodProperties) {
        if (methodProperties != null) {
            String propvalue = (String) methodProperties.get(propertyToSearch);
            if (StringUtils.isNotEmpty(propvalue)) {
                stringProp.add(propvalue);
            }
        }
    }

    @Override
    public IDomainAdaptor getGatheredDomain() {
        if (stringProp.isEmpty()) {
            // fake string domain it is because constrainer will be freezed with empty domain.
            stringProp.add("any");
        }
        EnumDomain strDomain = new EnumDomain(stringProp.toArray(new String[stringProp.size()]));
        return new EnumDomainAdaptor(strDomain);
    }
}
