package org.openl.meta.explanation;

import org.openl.base.INamedThing;
import org.openl.meta.IMetaHolder;

import java.util.Collection;

/**
 * Interface, contains operations over {@link ExplanationNumberValue} objects for explaining these objects in tree
 *
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public interface ExplanationForNumber<T extends ExplanationNumberValue<T>> extends IMetaHolder, INamedThing {

    String printValue();

    void setFullName(String name);

    void setName(String name);

    Collection<ExplanationNumberValue<?>> getChildren();

    T getObject();

    String getType();

    boolean isLeaf();
}
