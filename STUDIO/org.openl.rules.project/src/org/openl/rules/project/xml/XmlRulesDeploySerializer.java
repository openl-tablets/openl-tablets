package org.openl.rules.project.xml;

import java.io.InputStream;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.RulesDeploy;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XmlRulesDeploySerializer implements IRulesDeploySerializer {
    public final static String RULES_DEPLOY_DESCRIPTOR_TAG = "rules-deploy";

    private XStream xstream;

    public XmlRulesDeploySerializer() {
        xstream = new XStream(new DomDriver());
        xstream.omitField(RulesDeploy.class, "log");

        xstream.setMode(XStream.NO_REFERENCES);
        
        xstream.aliasType("publisher", RulesDeploy.PublisherType.class);
        xstream.aliasType(RULES_DEPLOY_DESCRIPTOR_TAG, RulesDeploy.class);
    }

    public XStream getXstream() {
        return xstream;
    }

    public String serialize(RulesDeploy source) {
        return xstream.toXML(source);
    }

    public RulesDeploy deserialize(InputStream source) {
        RulesDeploy rulesDeploy = (RulesDeploy) xstream.fromXML(source);
        postProcess(rulesDeploy);
        return rulesDeploy;
    }

    public RulesDeploy deserialize(String source) {
        RulesDeploy rulesDeploy = (RulesDeploy) xstream.fromXML(source);
        postProcess(rulesDeploy);
        return rulesDeploy;
    }

    private void postProcess(RulesDeploy descriptor) {
    }
}
