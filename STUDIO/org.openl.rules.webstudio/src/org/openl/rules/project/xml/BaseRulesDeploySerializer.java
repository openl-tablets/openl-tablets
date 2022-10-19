package org.openl.rules.project.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.model.ObjectVersionConverter;
import org.openl.rules.project.model.RulesDeploy;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class BaseRulesDeploySerializer<T> implements IRulesDeploySerializer {
    private final ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter;

    private final Marshaller jaxbMarshaller;
    private final Unmarshaller jaxbUnmarshaller;

    public BaseRulesDeploySerializer(ObjectVersionConverter<RulesDeploy, T> rulesDeployVersionConverter,
                                     Class<T> clazz) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // excludes header
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new OpenLSerializationException("Something went wrong during serialization");
        }

        this.rulesDeployVersionConverter = rulesDeployVersionConverter;
    }

    @Override
    public String serialize(RulesDeploy source) {
        try (StringWriter stringWriter = new StringWriter()) {
            jaxbMarshaller.marshal(rulesDeployVersionConverter.toOldVersion(source), stringWriter);
            return stringWriter.toString();
        } catch (IOException | JAXBException e) {
            throw new OpenLSerializationException("Something went wrong during Rules Deploy serialization");
        }
    }

    @Override
    public RulesDeploy deserialize(InputStream source) {
        try {
            @SuppressWarnings("unchecked")
            T oldVersion = (T) jaxbUnmarshaller.unmarshal(source);
            return rulesDeployVersionConverter.fromOldVersion(oldVersion);
        } catch (JAXBException e) {
            throw new OpenLSerializationException("Something went wrong when deserializing Rules Deploy");
        }
    }
}
