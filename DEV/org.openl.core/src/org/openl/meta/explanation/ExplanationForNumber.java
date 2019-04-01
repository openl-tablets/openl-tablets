package org.openl.meta.explanation;

import org.openl.base.INamedThing;
import org.openl.meta.IMetaHolder;
import org.openl.util.tree.ITreeElement;

/**
 * Interface, contains operations over {@link ExplanationNumberValue} objects for explaining these objects in tree(see
 * {@link ITreeElement})
 *
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public interface ExplanationForNumber<T extends ExplanationNumberValue<T>> extends IMetaHolder, INamedThing, ITreeElement<T> {

    String printValue();

    void setFullName(String name);

    void setName(String name);
}
