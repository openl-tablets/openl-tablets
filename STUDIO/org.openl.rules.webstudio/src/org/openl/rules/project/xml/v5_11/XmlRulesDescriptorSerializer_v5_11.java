package org.openl.rules.project.xml.v5_11;

import org.openl.rules.project.model.v5_11.RulesDeploy_v5_11;
import org.openl.rules.project.model.v5_11.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import javax.xml.bind.JAXBException;

public class XmlRulesDescriptorSerializer_v5_11 extends BaseRulesDeploySerializer<RulesDeploy_v5_11> {

    public XmlRulesDescriptorSerializer_v5_11() throws JAXBException {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_11.class);
    }
}
