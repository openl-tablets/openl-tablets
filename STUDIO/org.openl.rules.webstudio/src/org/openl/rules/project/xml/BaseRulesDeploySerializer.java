package org.openl.rules.project.xml;

import java.io.InputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;

public class BaseRulesDeploySerializer<T> implements IRulesDeploySerializer {
    protected final XStream xstream;
    private final ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter;

    public BaseRulesDeploySerializer(ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter) {
        xstream = new XStream(new DomDriver());
        this.rulesDeployVersionConverter = rulesDeployVersionConverter;
    }

    public String serialize(RulesDeploy source) {
        return xstream.toXML(rulesDeployVersionConverter.toOldVersion(source));
    }

    public RulesDeploy deserialize(InputStream source) {
        @SuppressWarnings("unchecked")
        T oldVersion = (T) xstream.fromXML(source);
        return rulesDeployVersionConverter.fromOldVersion(oldVersion);
    }

    public XStream getXstream() {
        return xstream;
    }
}
