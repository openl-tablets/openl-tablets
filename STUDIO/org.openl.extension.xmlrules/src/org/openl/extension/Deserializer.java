package org.openl.extension;

import java.io.InputStream;

public interface Deserializer<T> {
    T deserialize(InputStream source);
}
