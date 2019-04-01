package org.openl.ie.constrainer;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Set;

import org.openl.ie.constrainer.impl.IntSetEvent;

public interface IntSetVar extends Subject, IntSetEvent.IntSetEventConstants, java.io.Serializable {

    boolean bound();

    boolean contains(Set anotherSet);

    Goal generate();

    IntSetVar intersectionWith(IntSetVar anotherSet);

    boolean possible(int value);

    @Override
    void propagate() throws Failure;

    void remove(int val) throws Failure;

    void require(int val) throws Failure;

    boolean required(int value);

    Set requiredSet();

    IntSetVar unionWith(IntSetVar anotherSet);

    Set value() throws Failure;
}
