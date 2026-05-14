package org.openl.rules.project.model;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

class JAXBSerializer {
    private final JAXBContext jaxbContext;

    public JAXBSerializer(Class<?> clazz) {
        try {
            this.jaxbContext = JAXBContext.newInstance(clazz);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public void marshal(Object object, OutputStream outputStream) throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // excludes header
        marshaller.marshal(object, new SkipUntilBracketOutputStream(outputStream));
    }

    public Object unmarshal(InputStream inputStream) throws JAXBException {
        return jaxbContext.createUnmarshaller().unmarshal(inputStream);
    }

    static class SkipUntilBracketOutputStream extends FilterOutputStream {

        private boolean foundBracket = false;

        public SkipUntilBracketOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (foundBracket) {
                // The '<' has been found, write directly
                out.write(b);
            } else {
                // 0x3C — is the code of '<' symbol in ASCII and in UTF-8
                if (b == '<') {
                    foundBracket = true;
                    out.write(b);
                }
                // Skip other symbols till '<' will be found. usually it is a `\r` and `\n`
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (foundBracket) {
                // Performance optimization
                out.write(b, off, len);
            } else {
                // the beginning of the tag '<' is still not found
                for (int i = off; i < off + len; i++) {
                    if (foundBracket) {
                        out.write(b, i, off + len - i);
                        break;
                    } else {
                        write(b[i]);
                    }
                }
            }
        }
    }
}
