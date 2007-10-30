package org.openl.rules.commons.artefacts;

import java.io.InputStream;

public interface Content {
    InputStream getContent() throws ArtefactException;
}
