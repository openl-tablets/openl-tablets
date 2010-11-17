package org.openl.mapper.demo;

import com.exigen.chartis.chome.policy.domain.ChPolicyEntity;
import com.exigen.chartis.integration.acord.pcsurety.generated.HomePolicyQuoteInqRqType;
import com.exigen.chartis.integration.acord.pcsurety.generated.C25;
import com.exigen.ipb.base.datatypes.Term;
import com.exigen.ipb.policy.domain.PolicyDetail;
import org.openl.mapper.RulesBeanMapper;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.dozer.CustomConverter;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class Chartis2AcordModelMappingExample {

    public static void main(String[] args) throws URISyntaxException {

        URL url = Chartis2AcordModelMappingExample.class.getClassLoader().getResource("Chartis2AcordModelMappingExample.xlsx");
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

        ChPolicyEntity policy = new ChPolicyEntity();
        policy.setPolicyNumber("policy#1");
        policy.setVersion(1);

        Term term = new Term(new Date(), new Date());
        policy.setContractTerm(term);

        RulesBeanMapper mapper = new RulesBeanMapper(instanceClass, instance);

        HomePolicyQuoteInqRqType acord = HomePolicyQuoteInqRqType.Factory.newInstance();
        mapper.map(policy, acord);

        System.out.println(acord);
    }
}

