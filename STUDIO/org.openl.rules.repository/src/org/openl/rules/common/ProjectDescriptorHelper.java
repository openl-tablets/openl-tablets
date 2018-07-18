package org.openl.rules.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.CollectionUtils;
import org.openl.util.IOUtils;

/**
 * Created by ymolchan on 10/6/2014.
 */
public class ProjectDescriptorHelper {

    public static InputStream serialize(List<ProjectDescriptor> descriptors) {
        if (CollectionUtils.isEmpty(descriptors)) {
            return IOUtils.toInputStream("<descriptors/>");
        }
        StringBuilder builder = new StringBuilder("<descriptors>\n");

        for (ProjectDescriptor descriptor : descriptors) {
            builder.append("  <descriptor>\n");
            builder.append("    <projectName>").append(descriptor.getProjectName()).append("</projectName>\n");
            builder.append("    <projectVersion>").append(descriptor.getProjectVersion().getVersionName()).append(
                "</projectVersion>\n");
            builder.append("  </descriptor>\n");
        }
        builder.append("</descriptors>");
        return IOUtils.toInputStream(builder.toString());
    }

    @SuppressWarnings("unchecked")
    public static List<ProjectDescriptor> deserialize(InputStream source) {
        List<ProjectDescriptor> result = null;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            if (source.available() == 0) {
                return result;
            }
            XMLStreamReader streamReader = factory.createXMLStreamReader(source);

            while (streamReader.hasNext()) {
                streamReader.next();

                switch (streamReader.getEventType()) {
                    case XMLStreamReader.START_ELEMENT:
                        if (!"descriptor".equals(streamReader.getLocalName())) {
                            result = parseListOfDescripors(streamReader);
                        } else {
                            throw new IllegalStateException(
                                "An inappropriate element <" + streamReader.getLocalName() + ">");
                        }
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        throw new IllegalStateException(
                            "An inappropriate closing element </" + streamReader.getLocalName() + ">");
                    case XMLStreamReader.END_DOCUMENT:
                        return result;

                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("Unexpected end of the document");
    }

    private static List<ProjectDescriptor> parseListOfDescripors(
            XMLStreamReader streamReader) throws XMLStreamException {
        ArrayList<ProjectDescriptor> result = new ArrayList<>();
        while (streamReader.hasNext()) {
            streamReader.next();
            switch (streamReader.getEventType()) {
                case XMLStreamReader.START_ELEMENT:
                    if ("descriptor".equals(streamReader.getLocalName())) {
                        result.add(parseDescripor(streamReader));
                    } else {
                        throw new IllegalStateException(
                            "An inappropriate element <" + streamReader.getLocalName() + ">");
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (!"descriptors".equals(streamReader.getLocalName())) {
                        throw new IllegalStateException(
                            "An inappropriate closing element </" + streamReader.getLocalName() + ">");
                    }
                    return result;
            }
        }
        throw new IllegalStateException("Unexpected end of the document");
    }

    private static ProjectDescriptor parseDescripor(XMLStreamReader streamReader) throws XMLStreamException {
        String projectName = null;
        String projectVersion = null;
        while (streamReader.hasNext()) {
            streamReader.next();

            switch (streamReader.getEventType()) {
                case XMLStreamReader.START_ELEMENT:
                    String localName = streamReader.getLocalName();
                    if ("projectName".equals(localName)) {
                        projectName = parseElementAsString("projectName", streamReader);
                    } else if ("projectVersion".equals(localName)) {
                        projectVersion = parseElementAsString("projectVersion", streamReader);
                    } else {
                        throw new IllegalStateException("An inappropriate element <" + localName + ">");
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (!"descriptor".equals(streamReader.getLocalName())) {
                        throw new IllegalStateException(
                            "An inappropriate closing element </" + streamReader.getLocalName() + ">");
                    }
                    CommonVersionImpl commonVersion = new CommonVersionImpl(projectVersion);
                    return new ProjectDescriptorImpl(projectName, commonVersion);
            }
        }
        throw new IllegalStateException("Unexpected end of the document");
    }

    private static String parseElementAsString(String element, XMLStreamReader streamReader) throws XMLStreamException {
        String result = null;
        while (streamReader.hasNext()) {
            streamReader.next();

            switch (streamReader.getEventType()) {
                case XMLStreamReader.CHARACTERS:
                    result = streamReader.getText();
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if (!element.equals(streamReader.getLocalName())) {
                        throw new IllegalStateException(
                            "An inappropriate closing element </" + streamReader.getLocalName() + ">");
                    }
                    return result;
            }
        }
        throw new IllegalStateException("<" + element + "> element has not found");
    }
}
