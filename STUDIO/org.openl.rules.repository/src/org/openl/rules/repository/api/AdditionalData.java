package org.openl.rules.repository.api;

import java.util.function.UnaryOperator;

public interface AdditionalData<T extends AdditionalData> {
    T convertPaths(UnaryOperator<String> converter);
}
