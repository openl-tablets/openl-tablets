package org.openl.rules.project.xml.v5_11;

import org.openl.rules.project.model.v5_11.RulesDeploy_v5_11;
import org.openl.rules.project.model.v5_11.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

public class XmlRulesDescriptorSerializer_v5_11 extends BaseRulesDeploySerializer<RulesDeploy_v5_11> {

    public XmlRulesDescriptorSerializer_v5_11() {
        super(new RulesDeployVersionConverter(), RulesDeploy_v5_11.class);
    }
}
