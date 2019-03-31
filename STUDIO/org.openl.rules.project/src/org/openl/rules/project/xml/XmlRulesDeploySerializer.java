package org.openl.rules.project.xml;

import java.io.InputStream;

import com.thoughtworks.xstream.security.NoTypePermission;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.RulesDeploy;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XmlRulesDeploySerializer implements IRulesDeploySerializer {
    public final static String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";
    public final static String MODULE_NAME = "module";
    public final static String LAZY_MODULES_FOR_COMPILATION = "lazy-modules-for-compilation";

    private XStream xstream;

    public XmlRulesDeploySerializer() {
        xstream = new XStream(new DomDriver());
        xstream.addPermission(NoTypePermission.NONE);
        xstream.allowTypeHierarchy(String.class);
        xstream.allowTypeHierarchy(RulesDeploy.PublisherType.class);
        xstream.allowTypeHierarchy(RulesDeploy.class);
        xstream.allowTypeHierarchy(RulesDeploy.WildcardPattern.class);

        xstream.ignoreUnknownElements();
        xstream.omitField(RulesDeploy.class, "log");

        xstream.setMode(XStream.NO_REFERENCES);

        xstream.aliasType("publisher", RulesDeploy.PublisherType.class);
        xstream.aliasType(RULES_DEPLOY_DESCRIPTOR_TAG, RulesDeploy.class);
        xstream.aliasType(MODULE_NAME, RulesDeploy.WildcardPattern.class);

        xstream.aliasField(LAZY_MODULES_FOR_COMPILATION, RulesDeploy.class, "lazyModulesForCompilationPatterns");

        xstream.aliasField("name", RulesDeploy.WildcardPattern.class, "value");
        xstream.useAttributeFor(RulesDeploy.WildcardPattern.class, "value");
    }

    public XStream getXstream() {
        return xstream;
    }

    @Override
    public String serialize(RulesDeploy source) {
        return xstream.toXML(source);
    }

    @Override
    public RulesDeploy deserialize(InputStream source) {
        return (RulesDeploy) xstream.fromXML(source);
    }

    public RulesDeploy deserialize(String source) {
        return (RulesDeploy) xstream.fromXML(source);
    }

}
