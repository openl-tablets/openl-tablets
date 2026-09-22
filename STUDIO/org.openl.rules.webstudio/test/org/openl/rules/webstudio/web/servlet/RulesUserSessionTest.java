package org.openl.rules.webstudio.web.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class RulesUserSessionTest {

    @Test
    void serializationKeepsTheUserName() throws IOException, ClassNotFoundException {
        var session = new RulesUserSession();
        session.setUserName("john");

        var copy = (RulesUserSession) roundTrip(session);

        assertEquals("john", copy.getUserName());
        assertNull(copy.getWebStudio());
    }

    private static Object roundTrip(Object value) throws IOException, ClassNotFoundException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }
}
