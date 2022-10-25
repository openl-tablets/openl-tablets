package org.openl.rules.project.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

public class JAXBSerializer {
    private final Marshaller jaxbMarshaller;
    private final Unmarshaller jaxbUnmarshaller;

    public JAXBSerializer(Class clazz) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // excludes header
        jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    }

    public void marshal(Object object, Writer writer) throws JAXBException {
        jaxbMarshaller.marshal(object, writer);
    }

    public void marshal(Object object, OutputStream outputStream) throws JAXBException {
        jaxbMarshaller.marshal(object, outputStream);
    }

    public Object unmarshal(InputStream inputStream) throws JAXBException {
        return jaxbUnmarshaller.unmarshal(inputStream);
    }


}
