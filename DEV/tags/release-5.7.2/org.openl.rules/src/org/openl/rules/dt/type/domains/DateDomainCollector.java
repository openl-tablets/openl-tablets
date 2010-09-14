package org.openl.rules.dt.type.domains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.domain.DateRangeDomain;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class DateDomainCollector implements IDomainCollector {
    
    private Set<String> propertiesToSearch = new HashSet<String>();
    
    private List<Date> dateValues = new ArrayList<Date>();

    public DateDomainCollector() {        
    }
    
    public void gatherDomains(TableSyntaxNode tsn) {        
        ITableProperties tableProperties = tsn.getTableProperties();
        if (tableProperties != null) {
            for (String propertyName : propertiesToSearch) {            
                    Date propValue = (Date) tableProperties.getPropertyValue(propertyName);
                    if (propValue != null) {
                        dateValues.add(propValue);
                    }
            }
        }       
    }
    
    public IDomainAdaptor getGatheredDomain() {
        if (!dateValues.isEmpty()) {
            Collections.sort(dateValues);            
            DateRangeDomain domain = new DateRangeDomain(dateValues.get(0), dateValues.get(dateValues.size() - 1));
            return  new DateRangeDomainAdaptor(domain);
        } else {
            return null;
        }
    }
    
    public void addPropertyToSearch(String propertyToSearch) {
        propertiesToSearch.add(propertyToSearch);
    }
      

}
