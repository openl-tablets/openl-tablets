package org.openl.mapper.demo;

import com.exigen.chartis.domain.rating.Policy;
import com.exigen.ipb.base.datatypes.Term;
import com.exigen.ipb.policy.domain.PolicyEntity;
import org.openl.mapper.RulesBeanMapper;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

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

        RulesBeanMapper mapper = new RulesBeanMapper(instanceClass, instance);
        Policy ratingDomainPolicy = mapper.map(policy, Policy.class);

        System.out.println(ratingDomainPolicy);
    }
}
