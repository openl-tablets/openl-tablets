package org.openl.rules.repository.api;

public interface AdditionalData<T extends AdditionalData> {
    T convertPaths(PathConverter converter);
}
