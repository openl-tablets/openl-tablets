package org.openl.rules.project.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;

import javax.xml.bind.JAXBException;

public class BaseRulesDeploySerializer<T> implements IRulesDeploySerializer {
    private final ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter;

    private final JAXBSerializer jaxbSerializer;

    public BaseRulesDeploySerializer(ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter,
                                     Class<T> clazz) {
        jaxbSerializer = new JAXBSerializer(clazz);
        this.rulesDeployVersionConverter = rulesDeployVersionConverter;
    }

    @Override
    public String serialize(RulesDeploy source) throws IOException, JAXBException {
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbSerializer.marshal(rulesDeployVersionConverter.toOldVersion(source), stringWriter);
            return stringWriter.toString();
        }
    }

    @Override
    public RulesDeploy deserialize(InputStream source) throws JAXBException {
        @SuppressWarnings("unchecked")
        T oldVersion = (T) jaxbSerializer.unmarshal(source);
        return rulesDeployVersionConverter.fromOldVersion(oldVersion);
    }
}
