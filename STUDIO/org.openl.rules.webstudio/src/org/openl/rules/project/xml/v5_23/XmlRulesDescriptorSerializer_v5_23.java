package org.openl.rules.project.xml.v5_23;

import org.openl.rules.project.model.v5_23.RulesDeploy_v5_23;
import org.openl.rules.project.model.v5_23.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

public class XmlRulesDescriptorSerializer_v5_23 extends BaseRulesDeploySerializer<RulesDeploy_v5_23> {

    public XmlRulesDescriptorSerializer_v5_23() {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_23.class);
    }
}
