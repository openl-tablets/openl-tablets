package org.openl.rules.dt.type.domains;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openl.domain.StringDomain;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class StringDomainCollector implements IDomainCollector {
    
    private String propertyToSearch;
    
    Set<String> stringProp = new HashSet<String>();
    
    public StringDomainCollector(String propertyToSearch) {
        this.propertyToSearch = propertyToSearch;
    }

    public void gatherDomains(TableSyntaxNode tsn) {        
        ITableProperties tableProperties = tsn.getTableProperties();
        if (tableProperties != null) {
            String propvalue = (String) tableProperties.getPropertyValue(propertyToSearch);
            if (StringUtils.isNotEmpty(propvalue)) {                    
                stringProp.add(propvalue);
            }
       }
    }
    
    public IDomainAdaptor getGatheredDomain() {        
        if (stringProp.isEmpty()) {
            //fake string domain it is because constrainer will be freezed with empty domain.  
            stringProp.add("any");
        }
        StringDomain strDomain = new StringDomain(stringProp.toArray(new String[stringProp.size()]));
        return new EnumDomainAdaptor(strDomain);
    }

}
