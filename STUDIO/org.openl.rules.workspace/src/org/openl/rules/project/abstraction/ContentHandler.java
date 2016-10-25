package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.openl.rules.repository.api.FileData;

public interface ContentHandler {
    InputStream loadContent();
}
