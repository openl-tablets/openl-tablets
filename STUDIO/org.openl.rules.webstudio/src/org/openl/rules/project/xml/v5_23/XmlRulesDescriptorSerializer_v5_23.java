package org.openl.rules.project.xml.v5_23;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.v5_23.RulesDeploy_v5_23;
import org.openl.rules.project.model.v5_23.converter.RulesDeployVersionConverter;
import org.openl.rules.project.xml.BaseRulesDeploySerializer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;

public class XmlRulesDescriptorSerializer_v5_23 extends BaseRulesDeploySerializer<RulesDeploy_v5_23> {
    private static final String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";
    private static final String MODULE_NAME = "module";
    private static final String LAZY_MODULES_FOR_COMPILATION = "lazy-modules-for-compilation";

    public XmlRulesDescriptorSerializer_v5_23() {
        super(new RulesDeployVersionConverter());

        xstream.addPermission(NoTypePermission.NONE);
        xstream.allowTypeHierarchy(String.class);
        xstream.allowTypeHierarchy(RulesDeploy_v5_23.PublisherType.class);
        xstream.allowTypeHierarchy(RulesDeploy_v5_23.LogStorageType.class);
        xstream.allowTypeHierarchy(RulesDeploy_v5_23.class);
        xstream.allowTypeHierarchy(RulesDeploy_v5_23.WildcardPattern.class);

        xstream.ignoreUnknownElements();
        xstream.omitField(RulesDeploy.class, "log");

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType("publisher", RulesDeploy_v5_23.PublisherType.class);
        xstream.aliasType("storage", RulesDeploy_v5_23.LogStorageType.class);
        xstream.aliasType(RULES_DEPLOY_DESCRIPTOR_TAG, RulesDeploy_v5_23.class);
        xstream.aliasType(MODULE_NAME, RulesDeploy_v5_23.WildcardPattern.class);

        xstream.aliasField(LAZY_MODULES_FOR_COMPILATION, RulesDeploy_v5_23.class, "lazyModulesForCompilationPatterns");

        xstream.aliasField("name", RulesDeploy_v5_23.WildcardPattern.class, "value");
        xstream.useAttributeFor(RulesDeploy_v5_23.WildcardPattern.class, "value");
    }
}
