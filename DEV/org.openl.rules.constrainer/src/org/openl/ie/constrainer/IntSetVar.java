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

    public boolean bound();

    public boolean contains(Set anotherSet);

    public Goal generate();

    public IntSetVar intersectionWith(IntSetVar anotherSet);

    public boolean possible(int value);

    public void propagate() throws Failure;

    public void remove(int val) throws Failure;

    public void require(int val) throws Failure;

    public boolean required(int value);

    public Set requiredSet();

    public IntSetVar unionWith(IntSetVar anotherSet);

    public Set value() throws Failure;
}
