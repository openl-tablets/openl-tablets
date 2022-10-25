package org.openl.rules.project.xml.v5_15;

import org.openl.rules.project.model.v5_15.RulesDeploy_v5_15;
import org.openl.rules.project.model.v5_15.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import javax.xml.bind.JAXBException;

public class XmlRulesDescriptorSerializer_v5_15 extends BaseRulesDeploySerializer<RulesDeploy_v5_15> {

    public XmlRulesDescriptorSerializer_v5_15() throws JAXBException {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_15.class);
    }
}
