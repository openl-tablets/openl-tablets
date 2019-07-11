package org.openl.rules.ruleservice.core;

import java.io.IOException;
import java.io.InputStream;

public interface Resource {
    boolean exists();

    InputStream getResourceAsStream() throws IOException;
}
