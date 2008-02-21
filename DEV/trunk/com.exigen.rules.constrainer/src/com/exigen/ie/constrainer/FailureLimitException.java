package com.exigen.ie.constrainer;

/**
 * <p>Title: TimeLimitException</p>
 * <p>Description: This kind of exception are to be thrown by Constrainer
 * if the actual time of solution searching process exceeded the time limit.
 * The value for time limit could be set by calling</p>
 */

public class FailureLimitException extends RuntimeException
{

  private String _msg;
  private ChoicePointLabel _label = null;

  public FailureLimitException()
  {
    this("", null);
  }

  /**
   * Constructor for a TimeLimitException with a given description and label.
   */
  public FailureLimitException (String s, ChoicePointLabel label)
  {
    super(s);
    _msg = s;
    _label = label;
  }

  /**
   * Sets the description of this TimeLimitException.
   */
  void message(String s)
  {
    _msg = s;
  }

  /**
   * Returns the label.
   */
  public ChoicePointLabel label()
  {
    return _label;
  }

  /**
   * Returns description.
   */
  public String toString()
  {
    return "Failure: " + _msg;
  }

}