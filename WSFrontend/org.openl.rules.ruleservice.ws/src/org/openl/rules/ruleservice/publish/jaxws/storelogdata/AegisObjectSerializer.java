package org.openl.rules.ruleservice.publish.jaxws.storelogdata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.ProcessingException;

public class AegisObjectSerializer implements ObjectSerializer {

    private final AegisDatabinding aegisDatabinding;

    public AegisObjectSerializer(AegisDatabinding aegisDatabinding) {
        this.aegisDatabinding = aegisDatabinding;
    }

    @Override
    public String writeValueAsString(Object obj) throws ProcessingException {
        try {
            return marshal(obj);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private String marshal(Object obj) throws Exception {
        AegisContext context = aegisDatabinding.getAegisContext();
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        AegisType aegisType = context.getTypeMapping().getType(obj.getClass());

        @SuppressWarnings("squid:S2095") // no need to close ByteArrayOutputStream because of it does nothing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        XMLStreamWriter xmlWriter = null;
        try {
            xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);

            writer.write(obj,
                new QName("http://logging.ws.ruleservice.rules.openl.org", ""),
                false,
                xmlWriter,
                aegisType);

            return outputStream.toString();
        } finally {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
        }
    }

    @Override
    public <T> T readValue(String content, Class<T> type) throws ProcessingException {
        try {
            return readValueInternal(content, type);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readValueInternal(String content, Class<T> type) throws Exception {
        final AegisContext context = aegisDatabinding.getAegisContext();
        final AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        final AegisType aegisType = context.getTypeMapping().getType(type);

        XMLStreamReader xmlReader = null;
        try (InputStream source = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(source);
            return (T) reader.read(xmlReader, aegisType);
        } finally {
            if (xmlReader != null) {
                xmlReader.close();
            }
        }
    }
}
