package org.openl.rules.project.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class JAXBSerializer {
    private Marshaller jaxbMarshaller;
    private Unmarshaller jaxbUnmarshaller;

    private JAXBContext jaxbContext;
    private final Class clazz;

    public JAXBSerializer(Class clazz) {
        this.clazz = clazz;
    }

    public void marshal(Object object, Writer writer) throws JAXBException {
        getJaxbMarshaller().marshal(object, writer);
    }

    public void marshal(Object object, OutputStream outputStream) throws JAXBException {
        getJaxbMarshaller().marshal(object, outputStream);
    }

    public Object unmarshal(InputStream inputStream) throws JAXBException {
        return getJaxbUnmarshaller().unmarshal(inputStream);
    }

    private Marshaller getJaxbMarshaller() throws JAXBException {
        if (jaxbMarshaller == null) {
            jaxbMarshaller = getJaxbContext().createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // excludes header
        }
        return jaxbMarshaller;
    }

    private Unmarshaller getJaxbUnmarshaller() throws JAXBException {
        if (jaxbUnmarshaller == null) {
            jaxbUnmarshaller = getJaxbContext().createUnmarshaller();
        }
        return jaxbUnmarshaller;
    }

    private JAXBContext getJaxbContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(clazz);
        }
        return jaxbContext;
    }
}
