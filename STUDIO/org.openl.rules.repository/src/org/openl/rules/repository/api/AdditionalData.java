package org.openl.rules.repository.api;

import java.util.function.Function;

public interface AdditionalData<T extends AdditionalData> {
    T convertPaths(Function<String, String> converter);
}
