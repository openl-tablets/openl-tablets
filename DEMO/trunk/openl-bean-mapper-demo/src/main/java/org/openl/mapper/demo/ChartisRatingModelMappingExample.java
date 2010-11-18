package org.openl.mapper.demo;

import com.exigen.chartis.domain.rating.Policy;
import com.exigen.ipb.base.datatypes.Term;
import com.exigen.ipb.policy.domain.*;
import org.openl.mapper.RulesBeanMapper;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Arrays;
import java.math.BigDecimal;
import java.lang.reflect.Method;

public class ChartisRatingModelMappingExample {

    public static void main(String[] args) throws URISyntaxException {

        URL url = Chartis2AcordModelMappingExample.class.getClassLoader().getResource("ChartisRatingModelMappingExample.xlsx");
        File source = new File(url.toURI());
        ApiBasedRulesEngineFactory factory = new ApiBasedRulesEngineFactory(source);
        Class<?> instanceClass;
        Object instance;

        try {
            instanceClass = factory.getInterfaceClass();
            instance = factory.makeInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot load rules project", e);
        }

        PolicyEntity policy = new PolicyEntity();
        policy.setId(1L);
        policy.setInceptionDate(new Date());
        Term term = new Term();
        term.setEffective(new Date());
        term.setExpiration(new Date());

        policy.setContractTerm(term);

        PolicyDetail detail = new PolicyDetail();

        RiskItem riskItem1 = new RiskItem();
        riskItem1.setSeqNo(1);
        Coverage dwellCoverage = new Coverage();
        dwellCoverage.setCoverageCd("DWELL");
        dwellCoverage.setLimitAmount(new BigDecimal(10.5));
        riskItem1.setCoverages(Arrays.asList(dwellCoverage));

        Form form1 = new Form();
        form1.setFormCd("PCHO-ESOS");

        riskItem1.setForms(Arrays.asList(form1));

        detail.setRiskItems(Arrays.asList(riskItem1));

        policy.setPolicyDetail(detail);

        RulesBeanMapper mapper = new RulesBeanMapper(instanceClass, instance);
        Policy ratingDomainPolicy = mapper.map(policy, Policy.class);

        System.out.println("CorrelationId=" + ratingDomainPolicy.getCorrelationId());
        System.out.println("OriginalDate=" + ratingDomainPolicy.getOriginalDate());
        System.out.println("EffectiveDate=" + ratingDomainPolicy.getEffectiveDate());
        System.out.println("EffectiveYear=" + ratingDomainPolicy.getEffectiveYear());
        System.out.println("InsuredAmount=" + ratingDomainPolicy.getInsuredAmount());
        System.out.println("HasExclSpecEndorsement=" + ratingDomainPolicy.getHasExclSpecEndorsement());
    }
}
