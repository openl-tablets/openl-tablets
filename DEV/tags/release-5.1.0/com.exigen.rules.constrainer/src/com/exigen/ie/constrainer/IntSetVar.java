package com.exigen.ie.constrainer;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Set;

import com.exigen.ie.constrainer.impl.IntSetEvent;

public interface IntSetVar extends Subject,
                                   IntSetEvent.IntSetEventConstants,
                                   java.io.Serializable
{

  public boolean isPossible(int val);

  public boolean bound();

  public Set value() throws Failure;

  public Set requiredSet();

  public Set possibleSet();

  public Constraint nullIntersectWith(IntSetVar anotherVar);

  public IntSetVar intersectionWith(IntSetVar anotherSet);

  public IntSetVar unionWith(IntSetVar anotherSet);

  public Goal generate();

  public void propagate() throws Failure;

  public boolean possible(int value);

  public boolean required(int value);

  public boolean contains(Set anotherSet);

  public void remove(int val) throws Failure;

  public void require(int val) throws Failure;

  public IntExp cardinality();
}


