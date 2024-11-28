package org.openl.rules.project.model;

/**
 * Converter from old version of object to current version and vice-versa
 *
 * @param <C> Current version type
 * @param <O> Old version type
 */
public interface ObjectVersionConverter<C, O> {
    C fromOldVersion(O oldVersion);

    O toOldVersion(C currentVersion);
}
