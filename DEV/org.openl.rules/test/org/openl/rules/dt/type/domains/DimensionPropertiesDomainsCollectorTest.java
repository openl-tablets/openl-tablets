package org.openl.rules.dt.type.domains;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class DimensionPropertiesDomainsCollectorTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/validation/Dimension_Properties_Domains_Collector_Test.xls";

    private static final String PROPERTY_USREGION = "usregion";
    private static final String PROPERTY_LOB = "lob";
    private static final String PROPERTY_EFFECTIVE_DATE = "effectiveDate";
    private static final String PROPERTY_EXPIRATION_DATE = "expirationDate";
    private static final String PROPERTY_STATE = "state";
    private static final String PROPERTY_COUNTRY = "country";

    public DimensionPropertiesDomainsCollectorTest() {
        super(SRC);
    }

    @Test
    public void testDomainsGathering() {
        DimensionPropertiesDomainsCollector domainCollector = new DimensionPropertiesDomainsCollector();

        Map<String, IDomainAdaptor> propertiesDomains = domainCollector.gatherPropertiesDomains(getMethodsProperties());

        IDomainAdaptor usRegionDomainAdaptor = propertiesDomains.get(PROPERTY_USREGION);
        // number of values is 3. counting starts from 0.
        assertTrue(2 == usRegionDomainAdaptor.getMax());

        IDomainAdaptor lobDomainAdaptor = propertiesDomains.get(PROPERTY_LOB);
        // number of values is 5. counting starts from 0.
        assertTrue(4 == lobDomainAdaptor.getMax());

        IDomainAdaptor stateDomainAdaptor = propertiesDomains.get(PROPERTY_STATE);
        // number of values is 4. counting starts from 0.
        assertTrue(3 == stateDomainAdaptor.getMax());

        IDomainAdaptor countryDomainAdaptor = propertiesDomains.get(PROPERTY_COUNTRY);
        // number of values is 11. counting starts from 0.
        assertTrue(10 == countryDomainAdaptor.getMax());

        IDomainAdaptor effectiveDateDomainAdaptor = propertiesDomains.get(PROPERTY_EFFECTIVE_DATE);
        int effectiveDateMaxInd = effectiveDateDomainAdaptor.getMax();

        IDomainAdaptor expirationDateDomainAdaptor = propertiesDomains.get(PROPERTY_EXPIRATION_DATE);
        int expirationDateMaxInd = expirationDateDomainAdaptor.getMax();
        assertTrue(expirationDateMaxInd == effectiveDateMaxInd);

        IDomainAdaptor currentDateDomainAdaptor = propertiesDomains
            .get(DimensionPropertiesDomainsCollector.CURRENT_DATE_PARAM);
        int currentDateMaxInd = currentDateDomainAdaptor.getMax();
        assertTrue(currentDateMaxInd == expirationDateMaxInd);
    }

    private List<Map<String, Object>> getMethodsProperties() {
        List<Map<String, Object>> methodsProperties = new ArrayList<Map<String, Object>>();
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            ITableProperties props = tsn.getTableProperties();
            if (props != null) {
                methodsProperties.add(props.getAllProperties());
            }
        }
        return methodsProperties;
    }

}
