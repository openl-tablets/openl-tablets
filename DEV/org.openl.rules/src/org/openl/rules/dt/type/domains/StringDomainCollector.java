package org.openl.rules.dt.type.domains;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.domain.StringDomain;
import org.openl.util.StringUtils;

public class StringDomainCollector implements IDomainCollector {

    private String propertyToSearch;

    private Set<String> stringProp = new HashSet<>();

    public StringDomainCollector(String propertyToSearch) {
        this.propertyToSearch = propertyToSearch;
    }

    public void gatherDomains(Map<String, Object> methodProperties) {
        if (methodProperties != null) {
            String propvalue = (String) methodProperties.get(propertyToSearch);
            if (StringUtils.isNotEmpty(propvalue)) {
                stringProp.add(propvalue);
            }
        }
    }

    public IDomainAdaptor getGatheredDomain() {
        if (stringProp.isEmpty()) {
            // fake string domain it is because constrainer will be freezed with empty domain.
            stringProp.add("any");
        }
        StringDomain strDomain = new StringDomain(stringProp.toArray(new String[stringProp.size()]));
        return new EnumDomainAdaptor(strDomain);
    }
}
