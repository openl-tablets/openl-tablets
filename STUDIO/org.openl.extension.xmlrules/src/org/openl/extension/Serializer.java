package org.openl.extension;

import java.io.InputStream;

public interface Serializer<T> {
    String serialize(T object);

    T deserialize(InputStream source);
}
