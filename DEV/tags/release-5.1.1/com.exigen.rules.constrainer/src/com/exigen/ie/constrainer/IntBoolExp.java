package com.exigen.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
/**
 * An interface for the constraint boolean expression as an [0..1] integer expression
 * where 0 is false and 1 is true.
 */
public interface IntBoolExp extends IntExp
{
  /**
   * Returns true if the expression is false.
   * Note: this is not equals to <code>!isTrue()</code>
   */
  public boolean isFalse();

  /**
   * Returns true if the expression is true.
   * Note: this is not equals to <code>!isFalse()</code>
   */
  public boolean isTrue();

  /**
   * Sets the expression to be false.
   *
   * @throws Failure if expression is bound to be true.
   */
  public void setFalse() throws Failure;

  /**
   * Sets the expression to be true.
   *
   * @throws Failure if expression is bound to be false.
   */
  public void setTrue() throws Failure;

  /**
   * Returns the boolean expression: <code>(!this)</code>
   */
  public IntBoolExp not();

  /**
   * Returns the boolean expression: <code>(this || exp)</code>
   */
  public IntBoolExp or(IntBoolExp exp);

  /**
   * Returns the boolean expression: <code>(this || value)</code>
   */
  public IntBoolExp or(boolean value);

  /**
   * Returns the boolean expression: <code>(this && exp)</code>
   */
  public IntBoolExp and(IntBoolExp exp);

  /**
   * Returns the boolean expression: <code>(this && value)</code>
   */
  public IntBoolExp and(boolean value);

  /**
   * Returns the boolean expression: <code>(this -> exp)</code>
   */
  public IntBoolExp implies(IntBoolExp exp);

  /**
   * Returns the boolean expression: <code>(this -> value)</code>
   */
  public IntBoolExp implies(boolean value);
  
  
  public IntBoolExp eq(boolean b);

  /**
   * Returns the Constraint that corresponds to this expression.
   */
  public Constraint asConstraint();

} // ~IntBoolExp
