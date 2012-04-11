package org.openl.rules.dt.type.domains;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public interface IDomainCollector {

    void gatherDomains(TableSyntaxNode tsn);
    
    IDomainAdaptor getGatheredDomain();

}
